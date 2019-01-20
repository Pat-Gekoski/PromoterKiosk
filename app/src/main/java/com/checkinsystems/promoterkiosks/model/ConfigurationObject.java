package com.checkinsystems.promoterkiosks.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfigurationObject {

    private List<Customer> customers;
    private String systemID;
    private String password;
    private Date drawingDate;
    private boolean printTickets;

    public ConfigurationObject(String systemID, String password) {
        this.systemID = systemID;
        this.password = password;
        this.customers = new ArrayList<>();
        printTickets = true;
        drawingDate = new Date();
    }

    public String getSystemID() {
        return systemID;
    }

    public void setSystemID(String systemID) {
        this.systemID = systemID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public void addCustomer(Customer customer){
        customers.add(customer);
    }

    public Date getDrawingDate() {
        return drawingDate;
    }

    public void setDrawingDate(Date drawingDate) {
        this.drawingDate = drawingDate;
    }

    public boolean isPrintTickets() {
        return printTickets;
    }

    public void setPrintTickets(boolean printTickets) {
        this.printTickets = printTickets;
    }

    @Override
    public String toString() {
        return "ConfigurationObject{" +
                "customers=" + customers +
                ", systemID='" + systemID + '\'' +
                ", password='" + password + '\'' +
                ", drawingDate=" + drawingDate +
                ", printTickets=" + printTickets +
                '}';
    }
}
