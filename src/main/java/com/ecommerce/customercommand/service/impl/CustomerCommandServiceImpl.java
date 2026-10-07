package com.ecommerce.customercommand.service.impl;

import com.ecommerce.customercommand.dto.request.CustomerRegistrationRequest;
import com.ecommerce.customercommand.dto.request.CustomerUpdateRequest;
import com.ecommerce.customercommand.dto.response.CustomerResponse;
import com.ecommerce.customercommand.entity.Customer;
import com.ecommerce.customercommand.enums.CustomerStatus;
import com.ecommerce.customercommand.exception.CustomerNotFoundException;
import com.ecommerce.customercommand.exception.DuplicateEmailException;
import com.ecommerce.customercommand.exception.InvalidCustomerOperationException;
import com.ecommerce.customercommand.mapper.CustomerMapper;
import com.ecommerce.customercommand.repository.CustomerRepository;
import com.ecommerce.customercommand.service.CustomerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerCommandServiceImpl implements CustomerCommandService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Override
    @Transactional
    public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
        log.info("Customer registration requested");
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException(email);
        try {
            Customer saved = repository.save(mapper.toEntity(request));
            log.info("Customer registered with id {}", saved.getId());
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email);
        }
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        log.info("Customer update requested for id {}", customerId);
        Customer customer = repository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
        String email = request.email().trim().toLowerCase();
        repository.findByEmailIgnoreCase(email).filter(existing -> !Objects.equals(existing.getId(), customerId))
                .ifPresent(existing -> { throw new DuplicateEmailException(email); });
        if (customer.getStatus() == CustomerStatus.DELETED) {
            throw new InvalidCustomerOperationException("Deleted customers cannot be updated");
        }
        mapper.updateEntity(customer, request);
        Customer saved = repository.save(customer);
        log.info("Customer updated with id {}", customerId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long customerId) {
        log.info("Customer deletion requested for id {}", customerId);
        Customer customer = repository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (customer.getStatus() == CustomerStatus.DELETED) {
            throw new InvalidCustomerOperationException("Customer is already deleted");
        }
        customer.setStatus(CustomerStatus.DELETED);
        repository.save(customer);
        log.info("Customer soft deleted with id {}", customerId);
    }
}
