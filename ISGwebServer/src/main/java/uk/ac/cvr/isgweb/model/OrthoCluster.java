package uk.ac.cvr.isgweb.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OrthoCluster {

	private String orthoClusterID;
	private String orthoClusterIDShort = null;
	
	// key is species enum ordinal
	private Map<Species, List<SpeciesGene>> speciesToGenes = new EnumMap<Species, List<SpeciesGene>>(Species.class);

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

	public String getOrthoClusterIDShort() {
		if(orthoClusterIDShort == null) {
			orthoClusterIDShort = orthoClusterID.replaceAll("_ortho", "");
			for(Species species: Species.values()) {
				orthoClusterIDShort = orthoClusterIDShort.replaceAll(species.name(), species.getAbbreviation());
			}
		}
		return orthoClusterIDShort;
	}
	
}
