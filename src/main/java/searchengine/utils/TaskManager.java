package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

@Slf4j
@Component
@Setter @Getter
@RequiredArgsConstructor
public class TaskManager implements Runnable {

    private volatile boolean indexing;
    private final List<ForkJoinTask<Void>> taskList = new ArrayList<>();

    @Override
    public void run() {
        log.info("STARTED");
        while (!taskList.isEmpty()) {
            if (indexing) {
//                taskList.removeIf(ForkJoinTask::isDone);

//                for (Iterator<ForkJoinTask<Void>> iterator = taskList.iterator(); iterator.hasNext(); ) {
//                    ForkJoinTask<Void> task = iterator.next();
//                    if (task.isDone()) {
//                        iterator.remove();
//                    }
//                }

//                int count = 0;
//                for (int i = 0; i < taskList.size(); i++) {
//                    ForkJoinTask<Void> task = taskList.get(i);
//                    if (task.isDone()) {
//                        count++;
//                    }
//                }
//                if (taskList.size() == count) {
//                    taskList.clear();
//                }
            } else {
                taskList.forEach(t->t.cancel(true));
                taskList.clear();

//                for (Iterator<ForkJoinTask<Void>> iterator = taskList.iterator(); iterator.hasNext(); ) {
//                    ForkJoinTask<Void> task = iterator.next();
//                    boolean cancelled = task.cancel(true);
//                    log.info(task + " CANCELLED = " + cancelled);
//                    iterator.remove();
//                }
            }
        }
        log.info("FINISHED");
    }

    public synchronized void addTask(ForkJoinTask<Void> task) {
        taskList.add(task);
    }
}
