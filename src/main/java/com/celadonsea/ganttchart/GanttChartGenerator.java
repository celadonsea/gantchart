package com.celadonsea.ganttchart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GanttChartGenerator {

    private static final int LINE_HEIGHT = 30;

    private static final int CHART_LEFT_SIDE = 900;

    private static final int BOX_RATE = 2;
    private static final int DEFAULT_ESTIMATION = 40;

    private static final int HEADER_ROW_COUNT = 2;

    private int resourceCount = 1;
    private int lineNr = 0;
    private int featureCount = 0;
    private int rightSide;
    private List<Integer> slots = new ArrayList<>();
    private List<List<SlotLeak>> slotLeaks = new ArrayList<>();

    private class SlotLeak {
        SlotLeak(int start, int length) {
            this.start = start;
            this.length = length;
        }

        int start;
        int length;

        int getEnd() {
            return start + length;
        }
    }

    public String generate(String fileName, int resourceCount) throws IOException {
        this.resourceCount = resourceCount;
        for (int i = 0; i < resourceCount; i++) {
            slots.add(0);
            slotLeaks.add(new ArrayList<>());
        }
        List<Feature> features = parse(fileName);
        features.forEach(f -> logFeature(f, ""));

        return generateSVG(features);
    }

    private String generateSVG(List<Feature> features) {
        StringBuilder svgBody = new StringBuilder();
        features.forEach(feature -> svgBody.append(generateFeatureSVG(feature)));

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" standalone=\"no\"?>\r\n");
        svg.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\r\n");
        svg.append("\r\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" style=\"background: ");
        svg.append("white");
        svg.append(";\" width=\"")
                .append(CHART_LEFT_SIDE + 5 + rightSide * BOX_RATE)
                .append("\" height=\"")
                .append((featureCount + HEADER_ROW_COUNT + 5) * LINE_HEIGHT)
                .append("\" viewBox=\"0 0 ")
                .append(CHART_LEFT_SIDE + 5 + rightSide * BOX_RATE)
                .append(" ")
                .append((featureCount + HEADER_ROW_COUNT + 5) * LINE_HEIGHT)
                .append("\" onload=\"Init(evt)\">\r\n");

        svg.append(svgBody.toString());
        for (int i = 0; i < rightSide; i = i + 40) {
            svg.append("<line x1=\"")
                    .append(CHART_LEFT_SIDE + 5 + i * BOX_RATE)
                    .append("\" y1=\"")
                    .append(0)
                    .append("\" x2=\"")
                    .append(CHART_LEFT_SIDE + 5 + i * BOX_RATE)
                    .append("\" y2=\"")
                    .append((featureCount + HEADER_ROW_COUNT) * LINE_HEIGHT)
                    .append("\" style=\"fill:gray;stroke:gray;stroke-width:1;\"/>\n");
            svg
                    .append("<text x=\"")
                    .append(CHART_LEFT_SIDE + 5 + i * BOX_RATE + 10)
                    .append("\" y=\"")
                    .append(20)
                    .append("\" font-family=\"Arial\" font-size=\"20\">")
                    .append((i / 40 ) + 1 )
                    .append("</text>\r\n");
        }
        for (int i = 0; i < featureCount + HEADER_ROW_COUNT; i++) {
            svg
                    .append("<line x1=\"")
                    .append(5)
                    .append("\" y1=\"")
                    .append(i * LINE_HEIGHT)
                    .append("\" x2=\"")
                    .append(CHART_LEFT_SIDE + 5 + rightSide * BOX_RATE)
                    .append("\" y2=\"")
                    .append(i * LINE_HEIGHT)
                    .append("\" style=\"fill:gray;stroke:gray;stroke-width:1;\"/>\r\n");
        }
        svg
                .append("<text x=\"10\" y=\"")
                .append(20)
                .append("\" font-family=\"Arial\" font-size=\"20\">")
                .append("Weeks")
                .append("</text>\r\n");
        svg
                .append("<text x=\"10\" y=\"")
                .append(LINE_HEIGHT+ 20)
                .append("\" font-family=\"Arial\" font-size=\"20\">")
                .append("Resource utilization")
                .append("</text>\r\n");
        for (int i = 0; i < rightSide; i++) {
            int freeResources = freeResources(i);
            String color = "green";
            if (freeResources == 1) {
                color = "yellow";
            } else if (freeResources > 1) {
                color = "red";
            }
            svg
                    .append("<rect x=\"")
                    .append(CHART_LEFT_SIDE + 5 + i * BOX_RATE)
                    .append("\" y=\"")
                    .append(LINE_HEIGHT + 12)
                    .append("\" width=\"")
                    .append(BOX_RATE)
                    .append("\" height=\"")
                    .append(10)
                    .append("\" style=\"fill:")
                    .append(color)
                    .append(";stroke:")
                    .append(color)
                    .append(";stroke-width:1;\"/>\r\n");
        }

        svg.append("</svg>\r\n");
        return svg.toString();
    }

    private String generateFeatureSVG(Feature feature) {
        setRightSide(feature);
        StringBuilder featureSvg = new StringBuilder();
        lineNr++;
        int y = (lineNr + HEADER_ROW_COUNT - 1) * LINE_HEIGHT;
        featureSvg
                .append("<text x=\"10\" y=\"")
                .append(y + 20)
                .append("\" font-family=\"Arial\" font-size=\"20\">")
                .append(feature.getId())
                .append(": ")
                .append(feature.getName())
                .append(" (")
                .append(feature.getEstimation())
                .append(")")
                .append("</text>\r\n");
        int length = 50;
        String color = "red";

        if (feature.getEstimation() > 0) {
            length = feature.getEstimation() * BOX_RATE;
            color = "green";
        }
        if (feature.getTShirtSize() != null) {
            if (feature.getTShirtSize() != TShirtSize.NONE) {
                color = "blue";
            } else {
                color = "red";
            }
        }
        featureSvg
                .append("<rect x=\"")
                .append(CHART_LEFT_SIDE + 5 + feature.getStart() * BOX_RATE)
                .append("\" y=\"")
                .append(y + 8)
                .append("\" width=\"")
                .append(length)
                .append("\" height=\"")
                .append(15)
                .append("\" style=\"fill:")
                .append(color)
                .append(";stroke:")
                .append(color)
                .append(";stroke-width:1;\"/>\r\n");

        if (feature.getFollowers() != null) {
            feature.getFollowers().forEach(subFeature -> featureSvg.append(generateFeatureSVG(subFeature)));
        }

        return featureSvg.toString();
    }

    private void logFeature(Feature feature, String inline) {
        System.out.println(inline + feature.getId() + ": " + feature.getName() + "(" + feature.getEstimation() + " - " + feature.getStart() + ")");
        if (feature.getFollowers() != null) {
            feature.getFollowers().forEach(feature1 -> logFeature(feature1, inline + "    "));
        }
    }

    private List<Feature> parse(String fileName) throws IOException {
        Map<Integer, Feature> featureMap = new HashMap<>();
        Map<Integer, List<Feature>> temp = new HashMap<>();
        Set<Integer> followerSet = new HashSet<>();

        Path path = Paths.get(fileName);

        Stream<String> lines = Files.lines(path);

        Iterator<String> it = lines.iterator();

        it.next(); // header

        while (it.hasNext()) {
            String line = it.next();
            featureCount++;

            Feature feature = parseFeature(line);
            featureMap.put(feature.getId(), feature); // feature store

            if (temp.containsKey(feature.getId())) { // did it have a link before?
                System.out.println("USING TEMP FOR ID: " + feature.getId());
                feature.setFollowers(temp.get(feature.getId()));
                feature.getFollowers().forEach(fl -> {
                    fl.setStart(feature.getEnd());
                    followerSet.add(fl.getId());
                });
                temp.remove(feature.getId()); // remove from link map
            }

            if (feature.getPredecessors() != null) { // have predecessors?
                for (int predecessorId : feature.getPredecessors()) { // iterate through
                    if (featureMap.containsKey(predecessorId)) { // predecessor is known
                        featureMap.get(predecessorId).addFollower(feature);
                        feature.setStart(featureMap.get(predecessorId).getEnd());
                        followerSet.add(feature.getId());
                    } else { // predecessor is not known, put into the link map
                        if (!temp.containsKey(predecessorId)) {
                            temp.put(predecessorId, new ArrayList<>());
                        }
                        temp.get(predecessorId).add(feature);
                    }
                }
            }
        }

        List<Feature> featureList = featureMap.values().stream().filter(f -> !followerSet.contains(f.getId())).collect(Collectors.toList());
        startOptimization(featureList);
        return featureList;
    }

    private Feature parseFeature(String originalLine) {
        System.out.println("Original line: " + originalLine);
        String line = "";
        boolean inString = false;
        for (int pos = 0; pos < originalLine.length(); pos++) {
            if (originalLine.charAt(pos) == '"') {
                inString = !inString;
            }
            if (inString && originalLine.charAt(pos) == ',') {
                line += ';';
            } else {
                line += originalLine.charAt(pos);
            }
        }
        System.out.println("Process line: " + line.replaceAll("\"", ""));
        Feature feature = new Feature();
        String[] cells = line.replaceAll("\"", "").split(",");
        System.out.println("  Cell count: " + cells.length);
        feature.setId(Integer.parseInt(cells[0]));
        feature.setName(cells[3]);
        if (cells.length > 7) {
            String[] subCells = cells[7].split(";");
            for (int i = 0; i < subCells.length; i++) {
                if (subCells[i].endsWith(" size")) {
                    feature.setTShirtSize(TShirtSize.valueOf(subCells[i].split(" ")[0]));
                    if (feature.getEstimation() == 0) {
                        if (feature.getTShirtSize() == TShirtSize.S) {
                            feature.setEstimation(50);
                        } else if (feature.getTShirtSize() == TShirtSize.M) {
                            feature.setEstimation(100);
                        } else if (feature.getTShirtSize() == TShirtSize.L) {
                            feature.setEstimation(200);
                        } else if (feature.getTShirtSize() == TShirtSize.XL) {
                            feature.setEstimation(400);
                        } else if (feature.getTShirtSize() == TShirtSize.XXL) {
                            feature.setEstimation(800);
                        }
                    }
                }
            }
        }
        if (cells.length > 8) {
            if (!cells[8].isEmpty()) {
                int estimation = Integer.parseInt(cells[8].replaceAll(".00", ""));
                if (estimation > 0) {
                    feature.setTShirtSize(null);
                    feature.setEstimation(estimation);
                }
            }
        }
        if (cells.length > 9) {
            String[] subCells = cells[9].split(";");
            for (int i = 0; i < subCells.length; i++) {
                if (!subCells[i].isEmpty()) {
                    if (subCells[i].startsWith("Follows #")) {
                        feature.addPredecessor(Integer.parseInt(subCells[i].substring("Follows #".length())));
                    }
                }
            }
        }
        if (feature.getEstimation() == 0 && feature.getTShirtSize() == null) {
            feature.setTShirtSize(TShirtSize.NONE);
            feature.setEstimation(DEFAULT_ESTIMATION);
        }
        return feature;
    }

    private void startOptimization(List<Feature> features) {
        for (Feature feature : features) {
            getEarliestStart(feature);
            if (feature.getFollowers() != null) {
                startOptimization(feature.getFollowers());
            }
        }
    }

    /**
     * The smalles positive difference should win. If there is no positive difference
     * then the biggest negative difference should win.
     *
     * @param feature
     * @return
     */
    private void getEarliestStart(Feature feature) {
        System.out.println("Feature " + feature.getId() + " starts: " + feature.getStart() + " size: " + feature.getEstimation());
        boolean foundSlot = false;
        int smallestPositiveDifference = -1;
        int smallestPositiveDifferenceSlot = -1;
        int smallestPositiveDifferenceSlotLeak = -1;
        int biggestNegativeDifference = 1;
        int biggestNegativeDifferenceSlot = -1;
        int biggestNegativeDifferenceSlotLeak = -1;
        for (int slotNr = 0; slotNr < resourceCount; slotNr++) {
            for (int leakNr = 0; leakNr < slotLeaks.get(slotNr).size(); leakNr++) {
                SlotLeak slotLeak = slotLeaks.get(slotNr).get(leakNr);
                if (slotLeak.start >= feature.getStart() && slotLeak.length >= feature.getEstimation()) {
                    int currentDifference = feature.getStart() - slotLeak.start;
                    if (currentDifference < 0 && (biggestNegativeDifference == 1 || currentDifference > biggestNegativeDifference)) {
                        biggestNegativeDifference = currentDifference;
                        biggestNegativeDifferenceSlot = slotNr;
                        biggestNegativeDifferenceSlotLeak = leakNr;
                        foundSlot = true;
                    }
                    if (currentDifference >= 0 && (smallestPositiveDifference == -1 || currentDifference < smallestPositiveDifference)) {
                        smallestPositiveDifference = currentDifference;
                        smallestPositiveDifferenceSlot = slotNr;
                        smallestPositiveDifferenceSlotLeak = leakNr;
                        foundSlot = true;
                    }
                }
            }
        }

        if (foundSlot) {
            if (smallestPositiveDifference > -1) {
                SlotLeak selectedLeak = slotLeaks.get(smallestPositiveDifferenceSlot).get(smallestPositiveDifferenceSlotLeak);
                if (smallestPositiveDifference == 0) { // at leak start -> cut the left side
                    if (selectedLeak.length == feature.getEstimation()) {
                        slotLeaks.get(smallestPositiveDifferenceSlot).remove(smallestPositiveDifferenceSlotLeak);
                    } else {
                        selectedLeak.start = selectedLeak.start + feature.getEstimation();
                        selectedLeak.length = selectedLeak.length - feature.getEstimation();
                    }
                } else if (selectedLeak.getEnd() == (feature.getStart() + feature.getEstimation())) { // at leak end -> cut the right side
                    selectedLeak.length = selectedLeak.length - feature.getEstimation();
                } else { // at the middle of the leak --> cut the right side, and create a new for the rest
                    slotLeaks.get(smallestPositiveDifferenceSlot).add(new SlotLeak(feature.getStart() + feature.getEstimation(), selectedLeak.getEnd() - feature.getStart() - feature.getEstimation()));
                    selectedLeak.length = feature.getEstimation() - selectedLeak.start;

                }
                slots.set(smallestPositiveDifferenceSlot, feature.getStart() + feature.getEstimation());
                System.out.println("  selected slot: " + smallestPositiveDifferenceSlot);
            } else if (biggestNegativeDifference < 0) {
                SlotLeak selectedLeak = slotLeaks.get(biggestNegativeDifferenceSlot).get(biggestNegativeDifferenceSlotLeak);
                if (feature.getEstimation() == slotLeaks.get(biggestNegativeDifferenceSlot).get(biggestNegativeDifferenceSlotLeak).length) {
                    slotLeaks.get(biggestNegativeDifferenceSlot).remove(biggestNegativeDifferenceSlotLeak);
                } else {
                    selectedLeak.start = selectedLeak.start + feature.getEstimation();
                    selectedLeak.length = selectedLeak.length - feature.getEstimation();
                }
                feature.setStart(selectedLeak.start);
                System.out.println("  selected slot: " + biggestNegativeDifferenceSlot);
                System.out.println("  feaure new start: " + feature.getStart());
            } else {
                throw new IllegalStateException("Algorithm error :)");
            }
            return;
        }

        smallestPositiveDifference = -1;
        smallestPositiveDifferenceSlot = -1;
        biggestNegativeDifference = 1;
        biggestNegativeDifferenceSlot = -1;
        for (int slotNr = 0; slotNr < resourceCount; slotNr++) {
            int currentDifference = feature.getStart() - slots.get(slotNr);
            if (currentDifference < 0 && (biggestNegativeDifference == 1 || currentDifference > biggestNegativeDifference)) {
                biggestNegativeDifference = currentDifference;
                biggestNegativeDifferenceSlot = slotNr;
            }
            if (currentDifference >= 0 && (smallestPositiveDifference == -1 || currentDifference < smallestPositiveDifference)) {
                smallestPositiveDifference = currentDifference;
                smallestPositiveDifferenceSlot = slotNr;
            }
        }
        if (smallestPositiveDifference > -1) { // feature start >= slot end
            if (smallestPositiveDifference > 0) { // there is a space between slot end and feature start; create leak
                slotLeaks.get(smallestPositiveDifferenceSlot).add(new SlotLeak(slots.get(smallestPositiveDifferenceSlot), smallestPositiveDifference));
            }
            slots.set(smallestPositiveDifferenceSlot, feature.getStart() + feature.getEstimation()); // update slot end
            System.out.println("  selected slot: " + smallestPositiveDifferenceSlot);
        } else if (biggestNegativeDifference < 0) {
            feature.setStart(slots.get(biggestNegativeDifferenceSlot));
            slots.set(biggestNegativeDifferenceSlot, feature.getStart() + feature.getEstimation());
            System.out.println("  selected slot: " + biggestNegativeDifferenceSlot);
            System.out.println("  feature new start: " + feature.getStart());
        } else {
            throw new IllegalStateException("Algorithm error :)");
        }
        for (int slotNr = 0; slotNr < resourceCount; slotNr++) {
            System.out.println("## Slot " + slotNr + ": " + slots.get(slotNr));
        }
    }

    private void setRightSide(Feature feature) {
        if (feature.getEnd() > rightSide) {
            rightSide = feature.getEnd();
        }
    }

    private int freeResources(int day) {
        int freeResources = resourceCount;
        for (int i = 0; i < resourceCount; i++) {
            if (slots.get(i) >= day) {
                boolean isFree = false;
                for (int leakNr = 0; leakNr < slotLeaks.get(i).size(); leakNr++) {
                    SlotLeak slotLeak = slotLeaks.get(i).get(leakNr);
                    if (slotLeak.start <= day && slotLeak.getEnd() >= day) {
                        isFree = true;
                    }
                }
                if (!isFree) {
                    freeResources--;
                }
            }
        }
        return freeResources;
    }
}
