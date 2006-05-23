/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.io.IOException;
import java.util.Date;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;

import org.jfree.data.xy.XYSeries;

/**
 * 
 */
public class BasePeakDataRetrievalTask implements Task {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private RawDataFile rawDataFile;
    private BasePeakDataSet dataset;
    private int scanNumbers[];
    private int retrievedScans = 0;
    private TaskStatus status;
    private String errorMessage;

    /**
     * constructor for TIC
     * 
     * @param rawDataFile
     * @param scanNumbers
     * @param visualizer
     */
    BasePeakDataRetrievalTask(RawDataFile rawDataFile, int scanNumbers[],
            BasePeakDataSet dataset) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.dataset = dataset;
        this.scanNumbers = scanNumbers;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating TIC visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return (float) retrievedScans / scanNumbers.length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        // this task has no result
        return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        Scan scan;
        double basePeakIntensity, basePeakMZ;
        XYSeries series = dataset.getSeries(0);
        Date lastRedrawTime = new Date(), currentTime;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                
                scan = rawDataFile.getScan(scanNumbers[i]);
                basePeakMZ = scan.getBasePeakMZ();
                basePeakIntensity = scan.getBasePeakIntensity();

                // redraw every REDRAW_INTERVAL ms
                boolean notify = false;
                currentTime = new Date();
                if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
                    notify = true;
                    lastRedrawTime = currentTime;
                }

                // always redraw when we add last value
                if (i == scanNumbers.length - 1)
                    notify = true;

                series.add(scan.getRetentionTime() * 1000, basePeakIntensity,
                        notify);

                dataset.setMZValue(series.getItemCount() - 1, basePeakMZ);

            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            retrievedScans++;

        }

        status = TaskStatus.FINISHED;

    }

}
