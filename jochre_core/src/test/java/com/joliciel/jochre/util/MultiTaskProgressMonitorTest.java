///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.utils.Monitorable;
import com.joliciel.talismane.utils.MultiTaskProgressMonitor;
import com.joliciel.talismane.utils.ProgressMonitor;
import com.joliciel.talismane.utils.SimpleProgressMonitor;

public class MultiTaskProgressMonitorTest {
    private static final Log LOG = LogFactory.getLog(MultiTaskProgressMonitorTest.class);

    @Test
    public void testMonitor() {
    	MonitorableParentClass parentClass = new MonitorableParentClass();
    	ProgressMonitor progressMonitor = parentClass.monitorTask();
    	new Thread(parentClass).start();
    	while (!progressMonitor.isFinished()) {
    		LOG.debug("Progress: " + progressMonitor.getPercentComplete());
    	}
    }
    
    private static class MonitorableParentClass implements Runnable, Monitorable {
    	MultiTaskProgressMonitor monitor = null;
    	
		@Override
		public ProgressMonitor monitorTask() {
			monitor = new MultiTaskProgressMonitor();
			return monitor;
		}

		@Override
		public void run() {
			MonitorableChildClass childClass = new MonitorableChildClass();
			ProgressMonitor childMonitor = childClass.monitorTask();
			monitor.startTask(childMonitor, 0.4);
			childClass.run();
			monitor.endTask();
			
			childClass = new MonitorableChildClass();
			childMonitor = childClass.monitorTask();
			monitor.startTask(childMonitor, 0.6);
			childClass.run();
			monitor.endTask();
			
			monitor.setFinished(true);
			monitor.setFinished(true);
		}
    	
    }
    
    private static class MonitorableChildClass implements Monitorable {
    	SimpleProgressMonitor monitor = null;
		@Override
		public ProgressMonitor monitorTask() {
			monitor = new SimpleProgressMonitor();
			return monitor;
		}
		
		public void run() {
			monitor.setCurrentAction("blah");
			for (int i = 0; i<50000; i++) {
				monitor.setPercentComplete((double)i / 50000);
			}
		}
    	
		
    }
    
}
