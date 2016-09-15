package uk.ac.cvr.isgweb.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrthoCluster {

	private String orthoClusterID;
	private Map<Species, List<SpeciesGene>> speciesToGenes = new LinkedHashMap<Species, List<SpeciesGene>>();

	public OrthoCluster(String orthoClusterID) {
		super();
		this.orthoClusterID = orthoClusterID;
	}

	public String getOrthoClusterID() {
		return orthoClusterID;
	}

	public Map<Species, List<SpeciesGene>> getSpeciesToGenes() {
		return speciesToGenes;
	}
	
}
