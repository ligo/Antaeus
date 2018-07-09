package com.oceanai.quartz.schedule;

import com.oceanai.quartz.job.IndexMonitorJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class IndexSchedule {
    private Logger logger = LoggerFactory.getLogger(IndexSchedule.class);

    public void start(int intervals) {
        try {
            logger.info("-------Index Schedule Initializing -------------------");

            // First we must get a reference to a scheduler
            SchedulerFactory sf = new StdSchedulerFactory();
            Scheduler sched = sf.getScheduler();

            logger.info("-------Index Schedule Initialization Complete --------");

            logger.info("-------Scheduling Jobs ----------------");
            // job 1 will run At 00:05:00am every day
            JobKey jobKey = JobKey.jobKey("IndexMonitorJob", "group1");
            JobDetail job = newJob(IndexMonitorJob.class)
                    .withIdentity(jobKey)
                    .build();

            job.getJobDataMap().put("intervals", intervals);

            CronTrigger trigger = newTrigger()
                    .withIdentity("trigger1", "group1")
                    .withSchedule(cronSchedule("0 5 0 * * ?"))
                    .build();

            Date ft = sched.scheduleJob(job, trigger);
            logger.info(job.getKey() + " has been scheduled to run at: " + ft);
            logger.info("repeat at 00:05:00am every day, which based on quartz expression: " + trigger.getCronExpression());
            sched.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

/*    public static void main(String[] args) {
        IndexSchedule schedule = new IndexSchedule();
        schedule.start(5);
    }*/
}
