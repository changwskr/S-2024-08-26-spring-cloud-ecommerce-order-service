package com.example.orderservice.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.orderservice.transfer.dto.OrderDto;
import com.example.orderservice.business.dc.dao.model.OrderEntity;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.messagequeue.OrderProducer;
import com.example.orderservice.business.as.OrderServiceAS;
import com.example.orderservice.transfer.vo.Greeting;
import com.example.orderservice.web.transfer.RequestOrder;
import com.example.orderservice.web.transfer.ResponseCatalog;
import com.example.orderservice.web.transfer.ResponseOrder;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/order-service")
@Slf4j
public class OrderController {
	Environment env;
	OrderServiceAS orderService;
	KafkaProducer kafkaProducer;

	OrderProducer orderProducer;

	@Autowired
	private Greeting greeting;

	@Autowired
	public OrderController(Environment env, OrderServiceAS orderService, KafkaProducer kafkaProducer,
						   OrderProducer orderProducer) {
		this.env = env;
		this.orderService = orderService;
		this.kafkaProducer = kafkaProducer;
		this.orderProducer = orderProducer;
	}

	@GetMapping("/health_check")
	@Timed(value = "order.status", longTask = true)
	public String status() {
		return String.format("It's Working in Order Service on PORT %s", env.getProperty("local.server.port"));
	}

	@GetMapping("/welcome")
	@Timed(value = "order.welcome", longTask = true)
	public String welcome(HttpServletRequest request, HttpServletResponse response) {
		return greeting.getMessage();
	}


	// (POST) http://127.0.0.1:8000/order-service/(user-id)/orders
	@PostMapping("/{userId}/orders")
	public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
			@RequestBody RequestOrder orderDetails) {

		ResponseOrder responseOrder = null;

		log.info("★★★ Before retrieve orders microservice-createOrder");
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
		orderDto.setUserId(userId);

		// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
		// ■■■■■
		// 주문한 결과를 토픽을 이용해서 주문한 결과를 Producer한다.
		// 완료된 주문에 대한 Catalog-Service에 주문 수량을 조정하는 작업을 진행한다.

		boolean B_KFAKA_TOPIC_TYPE = false; // kafka - topic
		boolean B_KFAKA_SINC_TYPE = true; // kafka - topic + sinc event
		boolean B_KFAKA_REST_TEMPLATE_TYPE = false; // msa 동기화 방식 resttemplate, feign client

		if (B_KFAKA_TOPIC_TYPE) {

			// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
			// 상품id, 수량, 단가, 토탈금액, 유저id, 생성일자일시
			// orders TABLE { 상품id, 수량, 단가, 토탈금액, 유저id, 생성일자일시 }
			// catalog TABLE { id, 상품id, 상품명, 수량, 단가 }
			// 주문한 order 정보의 상품에 해당하는 수량만큼 catalog 정보의 수량(stock)를 조정한다.
			// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
			OrderDto createdOrder = orderService.createOrder2(orderDto);
			responseOrder = mapper.map(createdOrder, ResponseOrder.class);

			//--------------------------------------------------------------------------------------------------------------
			// send this order to the kafka
			// example-order-topic으로 pub하면 catalog-service에서 sub하는 방식이다.
			// 여기서는 메시징을 통해서 이벤트를 전달하는 방식이다.
			// 즉 컨슈머측에서 로직을 구사하는 방식이다.
			//--------------------------------------------------------------------------------------------------------------
			kafkaProducer.send("example-order-topic", orderDto);


		}
		else if (B_KFAKA_SINC_TYPE) {

			// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
			// 상품id, 수량, 단가, 토탈금액, 유저id, 생성일자일시
			// orders TABLE { 상품id, 수량, 단가, 토탈금액, 유저id, 생성일자일시 }
			// catalog TABLE { id, 상품id, 상품명, 수량, 단가 }
			// 주문한 order 정보의 상품에 해당하는 수량만큼 catalog 정보의 수량(stock)를 조정한다.
			// ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

			log.info("■■Orderservice.createOrder 주문을 넣는다. ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
			OrderDto createdOrder = orderService.createOrder2(orderDto);
			responseOrder = mapper.map(createdOrder, ResponseOrder.class);
			log.info("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");

			//--------------------------------------------------------------------------------------------------------------
			// 2번 스타일 여기서 example-order-topic을 pub하면 sink-connector에서 sub하는 방식이다.
			// 데이타베이스의 내용을 전달하는 방식이다. 그럼 변경 내용을 여기서 변경하여 내용을 전달한다. 그래서 UUID 부분과 물량을 여기서 조정하여
			// 순수하게 데이타만으로 처리하는 방식을 말한다.
			//--------------------------------------------------------------------------------------------------------------

			// ■■■■■
			// 2024-08-20 이 부분을 살린다.
			// 이 로직이 필요한 이유는 sink connector를 통해서 타 db에 저장목적일때 데이타를 채워주는 역할이다.
			// UUID 는 변경할 수 없으므로 여기서 다시 바꾸는 역할을 한다.
			// 여기서 UUID 을 셋팅해서 데이타를 직접 가공해서 Producer 한다.
			orderDto.setOrderId(UUID.randomUUID().toString());
			orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

			// sink connector를 통해서 가정은 현재 catalog-service의 데이타를 다른 데이타 베이스의 주문목록에 저장하는 것이
			// 목적이다.
			// 그래서 여기서는 uuid와 orderid, totalprice를 여기서 계산하는 것이다.

			log.info("★ OrderController.createOrder--------------------------------------------------------s---");
			log.info("★                 example-catalog-topic publish");
			log.info("★                 my_topic_orders publish");
			kafkaProducer.send("example-catalog-topic", orderDto);
			orderProducer.send("my_topic_orders", orderDto); // orders 의 토픽을 바라보고 있는 놈음 my-order-sink-connector이다.

			responseOrder = mapper.map(orderDto, ResponseOrder.class);

			log.info("★ OrderController.createOrder--------------------------------------------------------e---");

		}
		else if (B_KFAKA_REST_TEMPLATE_TYPE) {
			//--------------------------------------------------------------------------------------------------------------
			// 이것은 직접 orderservice가 성공하면 catalog 서비스를 호출하여 연동을 통한 상품수량을 조정하는 역할을 한다.
			// Using as rest template
			// RestTemplate.exchange(url, GET, 요청 데이타 타입, 수신 데이타 타입)
			// url: http://127.0.0.1:8000/catlog-service/catalogs (post)
			//--------------------------------------------------------------------------------------------------------------
		    RestTemplate restTemplate = new RestTemplate();
			List<ResponseCatalog> orders = new ArrayList<>();
			String catlogUrl = "http://127.0.0.1:8000/catlog-service/catalogs ";

			ResponseEntity<List<ResponseCatalog>> catalogListResponse =
										restTemplate.exchange(
												catlogUrl,
												HttpMethod.POST,
												null,
												new ParameterizedTypeReference<List<ResponseCatalog>>() {
										});

			List<ResponseCatalog> catalogList  = catalogListResponse.getBody();
			log.debug("--->catalogList>" + catalogList);
		}
		else {
			log.debug("아무 타입도 적용하지 않았다.");
		}

		// ■■■■■
		log.info("After added orders data");
		log.info("★★★ After retrieve orders microservice-createOrder-responseOrder-" + responseOrder);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);

	}

	@GetMapping("/{userId}/orders")
	public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception {
		log.info("★OrderController.getOrder---------------------Before retrieve orders microservice-getOrder");
		Iterable<OrderEntity> orderList = orderService.getOrdersByUserId2(userId);

		List<ResponseOrder> result = new ArrayList<>();
		orderList.forEach(v -> {
			result.add(new ModelMapper().map(v, ResponseOrder.class));
		});

		System.out.println("--->result>" + result);

		// 장애발생 테스트 목적으로 준비
		// 이것은 써킷브레이크가 제되로 작동되는지 확인하기 위해 만든 로직이다. 강제 타임아웃을 유도할 목적이다.
		boolean B_KFAKA_REST_TEMPLATE_TYPE = false; // msa 동기화 방식 resttemplate, feign client
		if (B_KFAKA_REST_TEMPLATE_TYPE) {
			try {
				// 2024-08-20 테스트 목적 막음 정상거래 유무 확인
				//Thread.sleep(30000);
				Thread.sleep(3000);
				 throw new Exception("장애 발생 - 의도적 목적 ");
			} catch (InterruptedException ex) {
				log.warn(ex.getMessage());
			}
		}

		log.info("Add retrieved orders data");
		log.info("★OrderController.getOrder-------------------------------★★ After retrieve orders microservice-getOrder");

		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
}
