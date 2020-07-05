package com.celadonsea.ganttchart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GanttApplication {

    public static void main(String[] args) {
        System.out.println("Gantt chart exporter");
        if (args.length != 3) {
            System.out.println("Usage: GanttApplication sourceFileName destinationFileName resourceCount");
            System.exit(2);
        }
        GanttChartGenerator ganttChartGenerator = new GanttChartGenerator();
        try {
            File file = new File(args[1]);
            if (file.exists()) {
                file.delete();
            }
            OutputStream os = new FileOutputStream(file);
            os.write(ganttChartGenerator.generate(args[0], Integer.parseInt(args[2])).getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
