/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package edu.harvard.hms.dbmi.bd2k.irct.join;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.harvard.hms.dbmi.bd2k.irct.exception.JoinActionSetupException;
import edu.harvard.hms.dbmi.bd2k.irct.model.join.Join;
import edu.harvard.hms.dbmi.bd2k.irct.model.join.JoinImplementation;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.Job;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.JobDataType;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.JobStatus;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.PersistableException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.ResultSetException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.ResultSet;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.ResultSetImpl;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;

/**
 * Performs a full outer join between two result sets
 * 
 * @author Jeremy R. Easton-Marks
 *
 */
public class FullOuterJoin implements JoinImplementation {

	@Override
	public void setup(Map<String, Object> parameters)
			throws JoinActionSetupException {
	}
	
	@Override
	public Job run(SecureSession session, Join join, Job result) throws ResultSetException, PersistableException {
		ResultSet leftResultSet = (ResultSet) join.getObjectValues().get(
				"LeftResultSet");

		if (leftResultSet == null) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("LeftResultSet is null");
			return result;
		}
		int leftColumnIndex;
		try {
			leftColumnIndex = leftResultSet.findColumn(join.getStringValues()
					.get("LeftColumn"));
		} catch (ResultSetException rse) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("LeftColumn : " + rse.getMessage());
			return result;
		}

		ResultSet rightResultSet = (ResultSet) join.getObjectValues().get(
				"RightResultSet");
		if (rightResultSet == null) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("RightResultSet is null");
			return result;
		}
		int rightColumnIndex;
		try {
			rightColumnIndex = rightResultSet.findColumn(join.getStringValues()
					.get("RightColumn"));
		} catch (ResultSetException rse) {
			result.setJobStatus(JobStatus.ERROR);
			result.setMessage("RightColumn : " + rse.getMessage());
			return result;
		}

		LeftOuterJoin leftOuterJoin = new LeftOuterJoin();
		
		ResultSetImpl computedResults = (ResultSetImpl) result.getData();
		int baseColumn = leftResultSet.getColumnSize();

		// Perform a left outer join to get all the left, and matches

		leftOuterJoin.run(session, join, result);
		computedResults = (ResultSetImpl) leftOuterJoin.getResults(result).getData();
		
		List<Integer> rightColumns = new ArrayList<Integer>();
		for (int rightColumnIterator = 0; rightColumnIterator < rightResultSet.getColumnSize(); rightColumnIterator++) {
			if (rightColumnIterator != rightColumnIndex) {
				rightColumns.add(rightColumnIterator);
			}
		}

		// Loop through right result to make sure that right joins occur bringing
		// in all right matches
		rightResultSet.beforeFirst();

		while (rightResultSet.next()) {
			Object rightRowMatchObj = ((ResultSetImpl) rightResultSet).getObject(rightColumnIndex);
			boolean match = false;

			// Reset resultset before looping through it
			leftResultSet.beforeFirst();
			while (leftResultSet.next()) {
				if (((ResultSetImpl) leftResultSet).getObject(leftColumnIndex).equals(rightRowMatchObj)) {
					match = true;
					break;
				}
			}

			// If a match isn't found then add it to the results
			if (!match) {
				// Add a new row
				computedResults.appendRow();
				// Set the join column value
				computedResults.updateObject(leftColumnIndex, rightRowMatchObj);

				// Copy Right values over
				for(int rightColumnIterator = 0; rightColumnIterator < rightColumns.size(); rightColumnIterator++) {
					computedResults.updateObject(baseColumn + rightColumnIterator, ((ResultSetImpl) rightResultSet).getObject(rightColumns.get(rightColumnIterator)));
				}

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
