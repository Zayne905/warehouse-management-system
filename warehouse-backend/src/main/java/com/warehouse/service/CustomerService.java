package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.CustomerMapper;
import com.warehouse.model.entity.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public List<Customer> list() {
        return customerMapper.selectList(
                new QueryWrapper<Customer>().orderByAsc("code"));
    }

    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Transactional
    public Customer save(Customer customer) {
        if (customer.getId() != null) {
            customerMapper.updateById(customer);
        } else {
            customerMapper.insert(customer);
        }
        return customer;
    }

    @Transactional
    public void delete(Long id) {
        customerMapper.deleteById(id);
    }
}
