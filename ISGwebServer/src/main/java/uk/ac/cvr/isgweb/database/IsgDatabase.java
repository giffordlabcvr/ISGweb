package uk.ac.cvr.isgweb.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;
import uk.ac.cvr.isgweb.text.TabularDataFile;

public class IsgDatabase {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isg.database");
	private static IsgDatabase instance;
	private Map<String, OrthoCluster> orthoClusterIndex = new LinkedHashMap<String, OrthoCluster>();
	private int expectedNumColumns;

	private IsgDatabase() {
		this.expectedNumColumns = expectedNumColumns();
		readBigTable();
	}
	
	public static IsgDatabase getInstance() {
		if(instance == null) {
			instance = new IsgDatabase();
		}
		return instance;
	}
	
	
	private void readBigTable() {
		String bigTableFileName = "Big_table_v3.4.txt";
		logger.info("Reading "+bigTableFileName);
		byte[] bigTableBytes = null;
		try {
			bigTableBytes = IOUtils.toByteArray(IsgDatabase.class.getResourceAsStream(bigTableFileName));
		} catch (IOException e) {
			throw new RuntimeException("Error reading Big Table: "+e.getMessage(), e);
		}
		TabularDataFile bigTableFile = new TabularDataFile();
		bigTableFile.populateFromBytes(bigTableBytes);
		List<String[]> rows = bigTableFile.getRows();
		logger.info("File "+bigTableFileName+" was read: "+rows.size()+" rows parsed");
		
		checkColumnNames(bigTableFile.getColumnNames());
		
		populateFromRows(bigTableFile.getRows());
	}
	
	private void populateFromRows(List<String[]> rows) {
		int rowIndex = 1;
		for(String[] row: rows) {
			try {
				populateFromRow(row);
			} catch(Exception e) {
				throw new RuntimeException("Error processing row "+rowIndex+": "+e.getMessage(), e);
			}

			rowIndex++;
		}
		
		
	}

	private void populateFromRow(String[] row) {
		if(row.length != expectedNumColumns) {
			throw new RuntimeException("Row had "+row.length+" columns, expected "+expectedNumColumns);
		}
		String orthoId = row[0];
		if(orthoId == null || orthoId.length() == 0) {
			throw new RuntimeException("OrthoID was null or empty");
		}
		OrthoCluster orthoCluster = orthoClusterIndex.computeIfAbsent(orthoId, id -> new OrthoCluster(id));
		for(Species species: Species.values()) {
			int ensembleIdColumnIndex = getColumnIndex(species, SpeciesGeneField.ensembleId);
			if(row[ensembleIdColumnIndex].equals("No other Ensembl ortholog")) {
				continue;
			}
			SpeciesGene speciesGene = new SpeciesGene();
			speciesGene.setSpecies(species);
			for(SpeciesGeneField speciesGeneField: SpeciesGeneField.values()) {
				int columnIndex = getColumnIndex(species, speciesGeneField);
				speciesGeneField.processCellValue(speciesGene, row[columnIndex]);
			}
			orthoCluster.getSpeciesToGenes()
				.computeIfAbsent(species, sp -> new ArrayList<SpeciesGene>())
				.add(speciesGene);
		}
	}

	private int getColumnIndex(Species species,
			SpeciesGeneField speciesGeneField) {
		int columnIndex = (species.ordinal() * SpeciesGeneField.values().length) + speciesGeneField.ordinal() + 1;
		return columnIndex;
	}

	private void checkColumnNames(String[] columnNames) {
		logger.info("Checking column names");
		if(columnNames == null) {
			throw new RuntimeException("bigTableFile columnNames was null");
		}
		if(columnNames.length == 0) {
			throw new RuntimeException("bigTableFile columnNames was empty");
		}
		if(columnNames[0].equals("OrthoID")) {
			logger.info("Confirmed column 0 was 'OrthoID'");
		} else {
			throw new RuntimeException("Expected column 0 to be 'OrthoID' but was '"+columnNames[0]+"'");
		}
		if(columnNames.length == expectedNumColumns) {
			logger.info("Confirmed number of columns was "+expectedNumColumns);
		} else {
			throw new RuntimeException("Expected "+expectedNumColumns+" columns but found "+columnNames.length);
		}
		for(Species species: Species.values()) {
			for(SpeciesGeneField speciesGeneField: SpeciesGeneField.values()) {
				String expectedColumnName = species.name()+" "+speciesGeneField.columnSuffix;
				int columnIndex = getColumnIndex(species, speciesGeneField);
				if(columnNames[columnIndex].equals(expectedColumnName)) {
					logger.info("Confirmed column "+columnIndex+" was '"+expectedColumnName+"'");
				} else {
					throw new RuntimeException("Expected column "+columnIndex+" to be '"+expectedColumnName+"' but found "+columnNames[columnIndex]);
				}
			}
		}
	}

	private int expectedNumColumns() {
		int expectedNumColumns = (Species.values().length * SpeciesGeneField.values().length) + 1;
		return expectedNumColumns;
	}

	private enum SpeciesGeneField {
		ensembleId("ENSEMBL_ID") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				if(cellValue.equals("No other Ensembl ortholog")) {
					return;
				}
				speciesGene.setEnsembleId(cellValue);
			}
		},
		geneName("Gene name") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				speciesGene.setGeneName(cellValue);
			}
		},
		dnDsRatio("dnds_ratio") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				if(cellValue.equals("no homology")) {
					speciesGene.setAnyHomology(false);
					return;
				}
				speciesGene.setDnDsRatio(Double.parseDouble(cellValue));
			}
		},
		percentIdentity("perc_id") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				if(cellValue.equals(speciesGene.getEnsembleId())) {
					return;
				}
				speciesGene.setPercentIdentity(Double.parseDouble(cellValue));
			}
		},
		log2foldChange("Log2FC") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				if(cellValue.equals("Not differentially expressed")) {
					speciesGene.setIsDifferentiallyExpressed(false);
					return;
				}
				speciesGene.setLog2foldChange(Double.parseDouble(cellValue));
			}
		},
		fdr("FDR") {
			@Override
			public void processCellValue(SpeciesGene speciesGene, String cellValue) {
				if(nullValue(cellValue)) {
					return;
				}
				if(cellValue.startsWith("Not differentially expresse")) {
					speciesGene.setIsDifferentiallyExpressed(false);
					return;
				}
				speciesGene.setFdr(Double.parseDouble(cellValue));
			}
		};

		private String columnSuffix;
		
		private SpeciesGeneField(String columnSuffix) {
			this.columnSuffix = columnSuffix;
		}

		protected boolean nullValue(String cellValue) {
			return cellValue == null || cellValue.length() == 0 || cellValue.equals("NA") || cellValue.equals("N/A") || cellValue.equals("n/a");
		}

		public abstract void processCellValue(SpeciesGene speciesGene, String cellValue);
		
	}

	public List<OrthoCluster> query(List<Criterion> criteria) {
		Stream<OrthoCluster> orthoClusterStream = orthoClusterIndex.values().stream();
		for(Criterion criterion: criteria) {
			orthoClusterStream = orthoClusterStream.filter(criterion::orthoClusterMatches);
		}
		return orthoClusterStream.collect(Collectors.toList());
	}
	
	
}
