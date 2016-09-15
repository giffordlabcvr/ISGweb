package uk.ac.cvr.isgweb.text;

import java.util.ArrayList;
import java.util.List;

public class TabularDataFile {

	private String[] columnNames;
	private List<String[]> rows = new ArrayList<String[]>();
	
	public void populateFromBytes(byte[] bytes) {
		String columnDelimiterRegex = "\\t";
		String inputString = new String(bytes);
		String[] allLines = inputString.split("\\r\\n|\\r|\\n");
		String headerLine = allLines[0];
		this.columnNames = headerLine.split(columnDelimiterRegex);
		for(int i = 0; i < this.columnNames.length; i++) {
			columnNames[i] = columnNames[i].trim();
		}
		
		for(int i = 1; i < allLines.length; i++) {
			String[] row = new String[columnNames.length];
			String line = allLines[i];
			if(line.replaceAll(columnDelimiterRegex, "").trim().isEmpty()) {
				continue; // only whitespace and column delimiters.
			}
			String[] rowValues = line.split(columnDelimiterRegex);
			for(int j = 0; j < rowValues.length; j++) {
				if(j < this.columnNames.length) {
					row[j] = rowValues[j].trim();
				}
			}
			this.rows.add(row);
		}
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public List<String[]> getRows() {
		return rows;
	}

}
