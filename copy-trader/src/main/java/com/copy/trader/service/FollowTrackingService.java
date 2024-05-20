package com.copy.trader.service;

import com.copy.common.dto.FollowReceiptTaskDto;
import com.copy.common.dto.FollowTaskDto;
import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.entity.SubscriptionEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.trader.handler.MessageHandler;
import com.copy.trader.producer.ReceiptProducer;
import com.copy.trader.task.SolanaTransactionTracker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.copy.trader.service.TrackingSessionService.DELIMITER;

@Service
@Slf4j
@RequiredArgsConstructor
@Data
public class FollowTrackingService {

    public static final String ADDRESS_NOT_FOUND = "Follow address not found";
    public static final String MAX_COLL_COUNT_RESEARCHED = ". Max coll count researched.";
    public static final String CONTACT_TO_ADMIN = "Unknown reason, pls contact to Admin.";
    public static final String NOT_TRACKING = "This key not tracking";
    @Autowired
    @Qualifier("solanaWebAddress")
    private String solanaWebAddress;

    @Qualifier("followTrackerExecutor")
    private final ThreadPoolTaskExecutor followTrackerExecutor;

    public static final String STOP_FOLLOWING = "Stop following";
    private final TrackingSessionService trackingSessionService;
    private final AuthRepository authRepository;
    private final FollowRepository followRepository;
    private final ReceiptProducer receiptProducer;

    @Autowired
    private final MessageHandler messageHandler;

    public void startFollow(FollowTaskDto dto){
        try{
            AuthEntity auth = dto.getFollow().getAuthEntity();
            String specialKey = auth.getAuthId() + DELIMITER + dto.getFollow().getFollowKeyWallet();

            Map<Boolean, String> allStatusesValid = isAllStatusesValidForStart(auth, dto.getFollow());
            if(allStatusesValid.containsKey(true)){
                dto.getFollow().setTrackingStatus(true);
                SolanaTransactionTracker newTracker = new SolanaTransactionTracker(dto.getFollow(), auth, solanaWebAddress, messageHandler);
                newTracker.setFollowTrackingService(this);
                trackingSessionService.putTracker(specialKey, newTracker);
                CompletableFuture.runAsync(newTracker::start, followTrackerExecutor);
                followRepository.save(dto.getFollow());
            }
            receiptProducer.produceReceipt(convertToDto(dto.getFollow(), dto, allStatusesValid));
        } catch (Exception e){
            log.error("Error while trying check if user already start follow: {}", e.getMessage());
        }
    }

    public void stopFollow(FollowTaskDto dto){
        try{
            AuthEntity auth = dto.getFollow().getAuthEntity();
            String specialKey = auth.getAuthId() + DELIMITER + dto.getFollow().getFollowKeyWallet();
            SolanaTransactionTracker tracker;
            String stopFollowReason;

            FollowReceiptTaskDto receiptDto = new FollowReceiptTaskDto();
            receiptDto.setStart(dto.isStart());
            receiptDto.setFollow(dto.getFollow());

            try{
                tracker = trackingSessionService.getTracker(specialKey);
                tracker.stop();
                if(dto.getFollow().getCountCollDone() >= auth.getSubscriptionEntity().getCountCollAvailable()){
                    stopFollowReason = MAX_COLL_COUNT_RESEARCHED;
                    receiptDto.setAnswer(Map.of(false, stopFollowReason));
                } else {
                    stopFollowReason = STOP_FOLLOWING;
                    receiptDto.setAnswer(Map.of(true, stopFollowReason));
                }
            } catch (NullPointerException e){
                stopFollowReason = NOT_TRACKING;
                receiptDto.setAnswer(Map.of(false, stopFollowReason));
            }
            receiptProducer.produceReceipt(receiptDto);
            stopAllFollowMarkers(dto.getFollow(), specialKey);
        } catch (Exception e){
            log.error("Error while trying check if user already stop follow: {}", e.getMessage());
        }
    }

    private Map<Boolean, String> isAllStatusesValidForStart(AuthEntity auth, FollowEntity currentFollow) {
        if (currentFollow != null){
            String specialKey = auth.getAuthId() + DELIMITER + currentFollow;
            if (trackingSessionService.containsTracker(specialKey)) {
                SolanaTransactionTracker tracker = trackingSessionService.getTracker(specialKey);
                if(tracker.getWebSocketSession().isOpen()){
                    return Map.of(false, "Tracking already in process");
                } else {
                    stopAllFollowMarkers(currentFollow, specialKey);
                    return Map.of(true, "Accept to start tracing");
                }
            } else {
                SubscriptionEntity currentSub = auth.getSubscriptionEntity();
                if (currentFollow.getCountCollDone() < currentSub.getCountCollAvailable()){
                    return Map.of(true, "Accept to start tracing");
                } else {
                    return Map.of(false, "Count of cols has max researched");
                }
            }
        }
        return Map.of(false, ADDRESS_NOT_FOUND);
    }

    private void stopAllFollowMarkers(FollowEntity currentFollow, String specialKey) {
        if(currentFollow.getTrackingStatus()){
            currentFollow.setTrackingStatus(false);
            followRepository.save(currentFollow);
        }
        trackingSessionService.removeTracker(specialKey);
    }

    private FollowReceiptTaskDto convertToDto(FollowEntity follow , FollowTaskDto dto, Map<Boolean, String> allStatusesValid) {
        return FollowReceiptTaskDto.builder()
                .follow(follow)
                .isStart(dto.isStart())
                .answer(allStatusesValid)
                .build();
    }
}
