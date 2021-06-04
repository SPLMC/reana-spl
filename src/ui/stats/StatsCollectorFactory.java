package ui.stats;

import paramwrapper.IModelCollector;
import tool.stats.IFormulaCollector;
import tool.stats.IMemoryCollector;
import tool.stats.IReuseCollector;
import tool.stats.ITimeCollector;
import tool.stats.NoopFormulaCollector;
import tool.stats.NoopMemoryCollector;
import tool.stats.NoopReuseCollector;
import tool.stats.NoopTimeCollector;

public class StatsCollectorFactory {

    private boolean collectionEnabled = false;

    public StatsCollectorFactory(boolean collectionEnabled) {
        this.collectionEnabled = collectionEnabled;
    }

    public ITimeCollector createTimeCollector() {
        if (collectionEnabled) {
            return new TimeCollector();
        } else {
            return new NoopTimeCollector();
        }
    }

    public IFormulaCollector createFormulaCollector() {
        if (collectionEnabled) {
            return new FormulaCollector();
        } else {
            return new NoopFormulaCollector();
        }
    }

    public IMemoryCollector createMemoryCollector() {
        if (collectionEnabled) {
            return new MemoryCollector();
        } else {
            return new NoopMemoryCollector();
        }
    }

    public IModelCollector createModelCollector() {
        if (collectionEnabled) {
            return new ModelCollector();
        } else {
            return null;
        }
    }

    public IReuseCollector createReuseCollector() {
        if (collectionEnabled) {
            return new ReuseCollector();
        } else {
            return new NoopReuseCollector();
        }
    }

}
