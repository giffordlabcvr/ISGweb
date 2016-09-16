package uk.ac.cvr.isgweb.database;

import java.util.List;

import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;

public class Criterion {

	public enum Presence {
		PRESENT,
		ABSENT
	}

	public enum SpeciesCategory {
		ALL,
		ANY,
		SPECIFIC
	}

	private Presence presence; // mandatory
	private SpeciesCategory speciesCategory; // mandatory
	
	private Species species; // must be non-null if speciesCategory == SPECIFIC, null otherwise.
	
	private Double minLog2FC; // null if presence == ABSENT, may be non-null otherwise
	private Double maxLog2FC; // null if presence == ABSENT, may be non-null otherwise
	private Double minFDR; // null if presence == ABSENT, may be non-null otherwise
	public Presence getPresence() {
		return presence;
	}
	public void setPresence(Presence presence) {
		this.presence = presence;
	}
	public SpeciesCategory getSpeciesCategory() {
		return speciesCategory;
	}
	public void setSpeciesCategory(SpeciesCategory speciesCategory) {
		this.speciesCategory = speciesCategory;
	}
	public Species getSpecies() {
		return species;
	}
	public void setSpecies(Species species) {
		this.species = species;
	}
	public Double getMinLog2FC() {
		return minLog2FC;
	}
	public void setMinLog2FC(Double minLog2FC) {
		this.minLog2FC = minLog2FC;
	}
	public Double getMaxLog2FC() {
		return maxLog2FC;
	}
	public void setMaxLog2FC(Double maxLog2FC) {
		this.maxLog2FC = maxLog2FC;
	}
	public Double getMinFDR() {
		return minFDR;
	}
	public void setMinFDR(Double minFDR) {
		this.minFDR = minFDR;
	}

	
	public boolean orthoClusterMatches(OrthoCluster orthoCluster) {
		switch(presence) {
		case PRESENT: {
			switch(speciesCategory) {
			case ALL: {
				return orthoCluster.getSpeciesToGenes().keySet().size() == Species.values().length;
			}
			case ANY: {
				return !orthoCluster.getSpeciesToGenes().isEmpty();
			}
			case SPECIFIC: {
				List<SpeciesGene> speciesGenes = orthoCluster.getSpeciesToGenes().get(species);
				return(speciesGenes != null && !speciesGenes.isEmpty());
			}
			}
		}
		case ABSENT: {
			switch(speciesCategory) {
			case ALL: {
				return orthoCluster.getSpeciesToGenes().keySet().size() < Species.values().length;
			}
			case ANY: {
				return orthoCluster.getSpeciesToGenes().isEmpty();
			}
			case SPECIFIC: {
				List<SpeciesGene> speciesGenes = orthoCluster.getSpeciesToGenes().get(species);
				return(speciesGenes == null || speciesGenes.isEmpty());
			}
			}
		}
		default: {
			throw new RuntimeException("Unexpected fall-through");
		}
		}
	}

	
	
}
