package de.Linus122.TimeIsMoney.data;


import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class PlayerData {
    // stores per payout data (key as defined in config.yml)
    private final HashMap<Integer, PayoutData> payoutData = new LinkedHashMap<>();
    
    public PayoutData getPayoutData(int payout_id) {
        if(payoutData.containsKey(payout_id)) {
            return payoutData.get(payout_id);
        }
        PayoutData payoutDataEntry = new PayoutData(0, new Date(), 0);
        
        payoutData.put(payout_id, payoutDataEntry);
        
        return payoutDataEntry;
    }

    public HashMap<Integer, PayoutData> getPayoutDataMap() {
        return this.payoutData;
    }

}
