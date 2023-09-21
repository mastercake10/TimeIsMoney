package de.Linus122.TimeIsMoney.data;

import org.apache.commons.lang.time.DateUtils;

import java.util.Date;

public class PayoutData {
    private double receivedToday = 0d;
    private Date lastPayoutDate;
    private int secondsSinceLastPayout = 0;

    public PayoutData(double receivedToday, Date lastPayoutDate, int secondsSinceLastPayout) {
        this.receivedToday = receivedToday;
        this.lastPayoutDate = lastPayoutDate;
        this.secondsSinceLastPayout = secondsSinceLastPayout;
    }

    public double getReceivedToday() {
        if(lastPayoutDate == null || !DateUtils.isSameDay(lastPayoutDate, new Date())) {
            // new day, reset total money received
            receivedToday = 0d;
        }
        return receivedToday;
    }


    /**
     * Sets the total amount of money received for today and updates the {@link #lastPayoutDate} variable to now.
     * @param receivedToday     Amount of money received today
     * @since 1.9.7
     */
    public void setReceivedToday(double receivedToday) {
        this.receivedToday = receivedToday;
        lastPayoutDate = new Date();
    }

    public int getSecondsSinceLastPayout() {
        return secondsSinceLastPayout;
    }

    public void setSecondsSinceLastPayout(int secondsSinceLastPayout) {
        this.secondsSinceLastPayout = secondsSinceLastPayout;
    }

    public Date getLastPayoutDate() {
        return lastPayoutDate;
    }

    public void setLastPayoutDate(Date lastPayoutDate) {
        this.lastPayoutDate = lastPayoutDate;
    }
}