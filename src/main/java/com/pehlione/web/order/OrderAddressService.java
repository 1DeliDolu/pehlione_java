package com.pehlione.web.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.address.UserAddress;

@Service
public class OrderAddressService {

	@Transactional
	public void attachShippingSnapshot(Order order, UserAddress address) {
		OrderShippingAddress snapshot = new OrderShippingAddress();
		snapshot.setOrder(order);
		snapshot.setFullName(address.getFullName());
		snapshot.setPhone(address.getPhone());
		snapshot.setLine1(address.getLine1());
		snapshot.setLine2(address.getLine2());
		snapshot.setCity(address.getCity());
		snapshot.setState(address.getState());
		snapshot.setPostalCode(address.getPostalCode());
		snapshot.setCountryCode(address.getCountryCode());
		order.setShippingAddress(snapshot);
	}
}
