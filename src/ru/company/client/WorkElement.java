package ru.company.client;


import java.io.Serializable;

public class WorkElement implements Serializable {
    public int index_i, index_j;
    boolean flag_doing,busy;

    public WorkElement(int index_i,int index_j,boolean flag_doing,boolean busy){
        this.index_i=index_i;
        this.index_j=index_j;
        this.flag_doing=flag_doing;
        this.busy=busy;

    }
}
