package com.example.orderservice.web.controller;

import com.example.orderservice.business.as.OrderServiceAS;
import com.example.orderservice.business.dc.dao.model.OrderEntity;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.messagequeue.OrderProducer;
import com.example.orderservice.transfer.dto.OrderDto;
import com.example.orderservice.transfer.vo.Greeting;
import com.example.orderservice.web.transfer.RequestOrder;
import com.example.orderservice.web.transfer.ResponseCatalog;
import com.example.orderservice.web.transfer.ResponseOrder;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order-service")
@Slf4j
public class OrderControllerOrg {
	Environment env;
	OrderServiceAS orderService;
	KafkaProducer kafkaProducer;

	OrderProducer orderProducer;

	@Autowired
	private Greeting greeting;

	@Autowired
	public OrderControllerOrg(Environment env, OrderServiceAS orderService, KafkaProducer kafkaProducer,
							  OrderProducer orderProducer) {
		this.env = env;
		this.orderService = orderService;
		this.kafkaProducer = kafkaProducer;
		this.orderProducer = orderProducer;
	}

	@GetMapping("/health_check_org")
	@Timed(value = "order.status", longTask = true)
	public String status() {
		return String.format("It's Working in Order Service on PORT %s", env.getProperty("local.server.port"));
	}

	@GetMapping("/welcome_org")
	@Timed(value = "order.welcome", longTask = true)
	public String welcome(HttpServletRequest request, HttpServletResponse response) {
		return greeting.getMessage();
	}


	// (post)http://127.0.0.1:8000/order-service/(user-id)/orders
	@PostMapping("/{userId}/orders_old")
	public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
			@RequestBody RequestOrder orderDetails) {

		log.info("★★★ Before retrieve orders microservice-createOrder");
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
		orderDto.setUserId(userId);
		/* jpa */

		/**
		 * 먼저
		 */
		OrderDto createdOrder = orderService.createOrder2(orderDto);

		ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

		/////////////////////////////////////////////////////////////////////////////////////
		// 기존에는 주문하면 주문을 하나 createOrder하는 것으로 끝났지만
		// 주문이 완료하기 위한 메시지 이벤트를 발생시켜
		// 주문 수량을 조정한다.
		/////////////////////////////////////////////////////////////////////////////////////

		boolean foo_t1 = false; // kafka - topic
		boolean foo_t2 = false; // kafka - topic + sinc event
		boolean foo_t3 = false; // msa 동기화 방식 resttemplate, feign client

		if (foo_t1) {
			/* send this order to the kafka */
			/*
			 * 여기서 example-order-topic 을 pub하면 catalog-service에서 sub하는 방식이다. 여기서는 메시징을 통해서
			 * 이벤트를 전달하는 방식이다. 즉 컨슈머측에서 로직을 구사하는 방식이다. 1번 2번을 동시에 하면 두번하는 방식이니 두개중 하나를 택한다.
			 */

			// 단순 상품목록의 카운터 조정의 역할이다.
			kafkaProducer.send("example-order-topic", orderDto);
		}
		else if (foo_t3) {

			/* 1) Using as rest template */
			// exchange(url,GET,requestEntity,받아오고자하는 데이타타입)
			// url: http://127.0.0.1:8000/catlog-service/orders (post)
		    RestTemplate restTemplate = new RestTemplate();
			List<ResponseCatalog> orders = new ArrayList<>();
			String catlogUrl = "http://127.0.0.1:8000/catlog-service/catalogs ";
							
			ResponseEntity<List<ResponseCatalog>> catalogListResponse = restTemplate.exchange(catlogUrl, HttpMethod.POST,
					null, new ParameterizedTypeReference<List<ResponseCatalog>>() {
					});

			List<ResponseCatalog> catalogList  = catalogListResponse.getBody();
			log.debug("--->catalogList>" + catalogList);
		}
		else if (foo_t2) {
			/*
			 * 2번 스타일 여기서 example-order-topic 을 pub하면 sink-connector에서 sub하는 방식이다. 여기서는
			 * 데이타베이스의 내용을 전달하는 방식이다. 그럼 변경 내용을 여기서 변경하여 내용을 전달한다. 그래서 UUID 부분과 물량을 여기서 조정하여
			 * 순수하게 데이타만으로 처리하는 방식을 말한다.
			 */

			// kafakaproducer의 목적은 단순이 상품목록에 대한 카운터 조정의 역할밖에 없다.
			kafkaProducer.send("example-catalog-topic", orderDto);
			
			
			// 이 로직이 필요한 이유는 sink connector를 통해서 타 db에 저장목적일때 데이타를 채워주는 역할이다.
			orderDto.setOrderId(UUID.randomUUID().toString());
			orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

			// 2022-11-22 컨테이너화 시키는데 sink 서비스는 일단 막는다.
			// 다음에는 푼다. 컨테이너에는 커넥터 서비스 제공환경 안만들었다.

			// sink connector를 통해서 가정은 현재 catalog-service의 데이타를 다른 데이타 베이스의 주문목록에 저장하는 것이
			// 목적이다.
			// 그래서 여기서는 uuid와 orderid, totalprice를 여기서 계산하는 것이다.
			orderProducer.send("my_sink_orders", orderDto);
		}
		else {

		}

		log.info("After added orders data");
		log.info("★★★ After retrieve orders microservice-createOrder");
		return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
	}

	@GetMapping("/{userId}/orders_org")
	public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception {
		log.info("★★★ Before retrieve orders microservice-getOrder");
		Iterable<OrderEntity> orderList = orderService.getOrdersByUserId2(userId);

		List<ResponseOrder> result = new ArrayList<>();
		orderList.forEach(v -> {
			result.add(new ModelMapper().map(v, ResponseOrder.class));
		});

		System.out.println("--->result>" + result);

		// 장애발생 테스트 목적으로 준비
		// 이것은 써킷브레이크가 제되로 작동되는지 확인하기 위해 만든 로직이다. 강제 타임아웃을 유도할 목적이다.
		boolean foo_t3 = false;
		if (foo_t3) {
			try {
				Thread.sleep(30000);
				throw new Exception("장애 발생");
			} catch (InterruptedException ex) {
				log.warn(ex.getMessage());
			}
		}

		log.info("Add retrieved orders data");
		log.info("★★★ After retrieve orders microservice-getOrder");

		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
}
