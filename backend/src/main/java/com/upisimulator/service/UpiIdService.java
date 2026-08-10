package com.upisimulator.service;

import com.upisimulator.dto.CreateUpiIdRequest;
import com.upisimulator.dto.UpiIdAvailabilityResponse;
import com.upisimulator.dto.UpiIdResponse;

import java.util.List;

public interface UpiIdService {

    UpiIdResponse createUpiId(Long userId, CreateUpiIdRequest request);

    List<UpiIdResponse> getUpiIds(Long userId);

    UpiIdAvailabilityResponse checkAvailability(String username);

    UpiIdResponse setDefault(Long userId, Long upiIdId);

}
