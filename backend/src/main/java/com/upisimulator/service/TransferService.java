package com.upisimulator.service;

import com.upisimulator.dto.ResolveRecipientResponse;
import com.upisimulator.dto.TransferRequest;
import com.upisimulator.dto.TransferResponse;

public interface TransferService {

    TransferResponse transfer(Long senderUserId, TransferRequest request);

    ResolveRecipientResponse resolveRecipient(String vpa);

}
