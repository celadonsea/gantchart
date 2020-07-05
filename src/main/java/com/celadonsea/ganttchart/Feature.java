package com.celadonsea.ganttchart;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Feature {
    private int id;
    private String name;
    private String targetVersion;
    private int estimation;
    private List<Feature> followers;
    private List<Integer> predecessors;
    private int start;
    private TShirtSize tShirtSize;

    public void addFollower(Feature follower) {
        if (followers == null) {
            followers = new ArrayList<>();
        }
        followers.add(follower);
    }

    public int getEnd() {
        return start + estimation;
    }

    public void addPredecessor(int predecessorId) {
        if (predecessors == null) {
            predecessors = new ArrayList<>();
        }
        predecessors.add(predecessorId);
    }

    public void setStart(int start) {
        if (start < this.start) {
            throw new IllegalArgumentException("Feature " + id + " update start from " + this.start + " to " + start);
        }
        int diff = start - this.start;
        System.out.println("-- feature " + id + " start updated from "+ this.start + " to " + start);
        this.start = start;
        if (followers != null && diff != 0) {
            for (Feature follower : followers) {
                follower.setStart(follower.getStart() + diff);
            }
        }
    }
}
