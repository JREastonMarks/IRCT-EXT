/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package edu.harvard.hms.dbmi.bd2k.irct.join;

import java.util.Map;

import edu.harvard.hms.dbmi.bd2k.irct.exception.JoinActionSetupException;
import edu.harvard.hms.dbmi.bd2k.irct.model.join.Join;
import edu.harvard.hms.dbmi.bd2k.irct.model.join.JoinImplementation;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.Job;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.JobDataType;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.JobStatus;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.PersistableException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.ResultSetException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.Column;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.ResultSet;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.ResultSetImpl;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;

/**
 * Performs a right outer join between two result sets
 * 
 * 
 * @author Jeremy R. Easton-Marks
 *
 */
public class UnionJoin implements JoinImplementation {
	@Override
	public void setup(Map<String, Object> parameters)
			throws JoinActionSetupException {
	}
	
	@Override
	public Job run(SecureSession session, Join join, Job result)
			throws ResultSetException, PersistableException {

		ResultSet leftResultSet = (ResultSet) join.getObjectValues().get(
				"LeftResultSet");

		if (leftResultSet == null) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("LeftResultSet is null");
			return result;
		}
		ResultSet rightResultSet = (ResultSet) join.getObjectValues().get(
				"RightResultSet");
		if (rightResultSet == null) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("RightResultSet is null");
			return result;
		}

		//Check that columns match
		if(leftResultSet.getColumnSize() != rightResultSet.getColumnSize()) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("Result sets have unequal number of columns");
			return result;
		}
		
		for (int columnIterator = 0; columnIterator < rightResultSet.getColumnSize(); columnIterator++) {
			Column rightColumn = rightResultSet.getColumn(columnIterator);
			Column leftColumn = leftResultSet.getColumn(columnIterator);
			if(!rightColumn.getName().equals(leftColumn.getName()) || (rightColumn.getDataType() != leftColumn.getDataType())) {
				result.setJobStatus(JobStatus.ERROR);
				result.setMessage("Left Column " + leftColumn.getName() + "(" + leftColumn.getDataType() + ") is not equal to Right Column " + rightColumn.getName() + "(" + rightColumn.getDataType() + ")");
				return result;
			}
		}
		
		
		//Create new computed results with the same columns
		ResultSetImpl computedResults = (ResultSetImpl) result.getData();
		
		for (int rightColumnIterator = 0; rightColumnIterator < rightResultSet.getColumnSize(); rightColumnIterator++) {
				computedResults.appendColumn(rightResultSet.getColumn(rightColumnIterator));
		}
		
		// Loop through the Left Result Set
		leftResultSet.beforeFirst();
		while(leftResultSet.next()) {
			computedResults.appendRow();
			for (int leftColumnIterator = 0; leftColumnIterator < leftResultSet.getColumnSize(); leftColumnIterator++) {
				computedResults.updateObject(leftColumnIterator, ((ResultSetImpl) leftResultSet).getObject(leftColumnIterator));
			}
		}
		
		// Loop through the Right Result Set
		rightResultSet.beforeFirst();
		while(rightResultSet.next()) {
			computedResults.appendRow();
			for (int rightColumnIterator = 0; rightColumnIterator < rightResultSet.getColumnSize(); rightColumnIterator++) {
				computedResults.updateObject(rightColumnIterator, ((ResultSetImpl) rightResultSet).getObject(rightColumnIterator));
			}
		}

		computedResults.beforeFirst();

		result.setJobStatus(JobStatus.COMPLETE);
		result.setData(computedResults);
		return result;
	}

	@Override
	public Job getResults(Job result) {
		return result;
	}

	@Override
	public JobDataType getJoinDataType() {
		return JobDataType.TABULAR;
	}
}
