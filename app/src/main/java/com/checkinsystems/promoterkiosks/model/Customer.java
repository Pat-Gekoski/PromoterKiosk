package com.checkinsystems.promoterkiosks.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Customer {

    private String name;
    private String email;
    private String indate;
    private int ticket;


    public Customer(String name, String email) {
        this.name = name;
        this.email = email;

        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
        indate = s.format(new Date().getTime());

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTicket() {
        return ticket;
    }

    public void setTicket(int ticket) {
        this.ticket = ticket;
    }

    public String getDate() {
        return indate;
    }
}
