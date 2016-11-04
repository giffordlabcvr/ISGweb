package uk.ac.cvr.isgweb.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import uk.ac.cvr.isgweb.SearchByNumberCriteria;
import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;
import uk.ac.cvr.isgweb.text.TabularDataFile;

public class IsgDatabase {

	public static Logger logger = Logger.getLogger("uk.ac.cvr.isg.database");
	private static IsgDatabase instance;
	// orthoCluster ID to orthoCluster
	private Map<String, OrthoCluster> orthoClusterIndex = new LinkedHashMap<String, OrthoCluster>();
	// ENSEMBL ID to list of species genes
	private Map<String, List<SpeciesGene>> ensemblIdToSpeciesGenes = new LinkedHashMap<String, List<SpeciesGene>>();
	// Gene name to list of ENSEMBL IDs
	private Map<String, Set<String>> geneNameToEnsemblIds = new LinkedHashMap<String, Set<String>>();
	private Map<String, Set<String>> lowerCaseGeneNameToEnsemblIds = new LinkedHashMap<String, Set<String>>();
	
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
		String bigTableFileName = "Big_table_v3.6.1.xls";
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
			String ensembleId = speciesGene.getEnsembleId();
			List<SpeciesGene> speciesGenesWithEnsemblId = 
				ensemblIdToSpeciesGenes.computeIfAbsent(ensembleId, ensId -> new ArrayList<SpeciesGene>());

//			if(speciesGene.getSpecies() != Species.gallus_gallus && speciesGenesWithEnsemblId.size() > 0) {
//			throw new RuntimeException("Multiple SpeciesGene instances found for non-chicken ENSEMBL ID "+ensembleId);
//			}
			
			for(SpeciesGene existingGene: speciesGenesWithEnsemblId) {
				if(!equalFieldValues(existingGene.getGeneName(), speciesGene.getGeneName())) {
					throw new RuntimeException("Multiple SpeciesGene instances with ENSEMBL ID "+ensembleId+" have different Gene names");
				}
				if(!equalFieldValues(existingGene.getLog2foldChange(), speciesGene.getLog2foldChange())) {
					throw new RuntimeException("Multiple SpeciesGene instances with ENSEMBL ID "+ensembleId+" have different Log2FC values");
				}
				if(!equalFieldValues(existingGene.getFdr(), speciesGene.getFdr())) {
					throw new RuntimeException("Multiple SpeciesGene instances with ENSEMBL ID "+ensembleId+" have different FDR values");
				}
			}
			
			speciesGenesWithEnsemblId.add(speciesGene);
			
			
			orthoCluster.getSpeciesToGenes()
				.computeIfAbsent(species, sp -> new ArrayList<SpeciesGene>())
				.add(speciesGene);

			speciesGene.getOrthoClusters().add(orthoCluster);
			
			String geneName = speciesGene.getGeneName();
			if(geneName != null) {
				geneNameToEnsemblIds
					.computeIfAbsent(geneName, gn -> new LinkedHashSet<String>())
					.add(speciesGene.getEnsembleId());
				lowerCaseGeneNameToEnsemblIds
					.computeIfAbsent(geneName.toLowerCase(), gn -> new LinkedHashSet<String>())
					.add(speciesGene.getEnsembleId());

			}
				
		}
	}
	
	private boolean equalFieldValues(Object value1, Object value2) {
		if(value1 == null) {
			return value2 == null;
		}
		if(value2 == null) {
			return value1 == null;
		}
		return value1.equals(value2);
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
				if(cellValue.equals("Not differentially expressed")) {
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

	public List<OrthoCluster> queryBySpeciesCriteria(List<SpeciesCriterion> speciesCriteria, GeneRegulationParams geneRegulationParams) {
		Stream<OrthoCluster> orthoClusterStream = orthoClusterIndex.values().stream();
		for(SpeciesCriterion speciesCriterion: speciesCriteria) {
			orthoClusterStream = orthoClusterStream.filter(cluster -> {
				return speciesCriterion.orthoClusterSatisfiesCriterion(geneRegulationParams, cluster);	
			});
		}
		return orthoClusterStream.collect(Collectors.toList());
	}

	public List<OrthoCluster> queryBySpeciesNumber(
			SearchByNumberCriteria criteria,
			GeneRegulationParams geneRegulationParams) {
		return orthoClusterIndex.values().stream()
			.filter(cluster -> {
				int numPresent = 0;
				int numUpRegulatedPresent = 0;
				int numDownRegulatedPresent = 0;
				Map<Species, List<SpeciesGene>> speciesToGenes = cluster.getSpeciesToGenes();
				for(Species species: Species.values()) {
					List<SpeciesGene> genes = speciesToGenes.get(species);
					if(genes == null || genes.isEmpty()) {
						continue;
					} else {
						numPresent++;
					}
					for(SpeciesGene gene: genes) {
						if(geneRegulationParams.upregulatedGene(gene)) {
							numUpRegulatedPresent++;
							break;
						}
					}
					for(SpeciesGene gene: genes) {
						if(geneRegulationParams.downregulatedGene(gene)) {
							numDownRegulatedPresent++;
							break;
						}
					}
				}
				if(
						numPresent < criteria.getPresentMin() ||
						numPresent > criteria.getPresentMax() ||
						numUpRegulatedPresent < criteria.getUpRegulatedPresentMin() ||
						numUpRegulatedPresent > criteria.getUpRegulatedPresentMax() ||
						numDownRegulatedPresent < criteria.getDownRegulatedPresentMin() ||
						numDownRegulatedPresent > criteria.getDownRegulatedPresentMax()) {
					return false;
				}
				return true;
			})
			.collect(Collectors.toList());
	}

	
	public Map<String, List<SpeciesGene>> getEnsemblIdToSpeciesGenes() {
		return ensemblIdToSpeciesGenes;
	}

	public Map<String, Set<String>> getGeneNameToEnsemblIds() {
		return geneNameToEnsemblIds;
	}

	public List<OrthoCluster> queryByGeneNameOrEnsemblId(String geneNameOrEnsemblId) {
		Set<String> ensemblIds = geneNameToEnsemblIds.get(geneNameOrEnsemblId);
		if(ensemblIds == null) {
			ensemblIds = lowerCaseGeneNameToEnsemblIds.get(geneNameOrEnsemblId.toLowerCase());
		}
		
		if(ensemblIds == null) {
			List<SpeciesGene> speciesGenes = ensemblIdToSpeciesGenes.get(geneNameOrEnsemblId);
			if(speciesGenes == null) {
				return Collections.emptyList();
			} else {
				return new ArrayList<OrthoCluster>(
						speciesGenes.stream()
						.map(gene -> gene.getOrthoClusters())
						.flatMap(clusterList -> clusterList.stream())
						.collect(Collectors.toSet()));
			}
		} else {
			return new ArrayList<OrthoCluster>(
					ensemblIds.stream()
					.map(eid -> ensemblIdToSpeciesGenes.get(eid))
					.flatMap(geneList -> geneList.stream())
					.map(gene -> gene.getOrthoClusters())
					.flatMap(clusterList -> clusterList.stream())
					.collect(Collectors.toSet()));
		}
	}


	
	
	
}
