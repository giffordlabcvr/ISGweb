package uk.ac.cvr.isgweb.database;

import java.util.List;

import uk.ac.cvr.isgweb.model.OrthoCluster;
import uk.ac.cvr.isgweb.model.Species;
import uk.ac.cvr.isgweb.model.SpeciesGene;

public class SpeciesCriterion {

	private Species species; 

	// at most one of these is true
	// if either is true, this implies requirePresent
	private boolean requireUpregulated;
	private boolean requireNotUpregulated;

	// at most one of these is true
	// if either is true, this implies requirePresent
	private boolean requireDownregulated;
	private boolean requireNotDownregulated;

	// at most one of these is true
	private boolean requirePresent;
	private boolean requireAbsent;
	
	
	public void setSpecies(Species species) {
		this.species = species;
	}
	public void setRequireUpregulated(boolean requireUpregulated) {
		this.requireUpregulated = requireUpregulated;
	}
	public void setRequireNotUpregulated(boolean requireNotUpregulated) {
		this.requireNotUpregulated = requireNotUpregulated;
	}
	public void setRequireDownregulated(boolean requireDownregulated) {
		this.requireDownregulated = requireDownregulated;
	}
	public void setRequireNotDownregulated(boolean requireNotDownregulated) {
		this.requireNotDownregulated = requireNotDownregulated;
	}
	public void setRequirePresent(boolean requirePresent) {
		this.requirePresent = requirePresent;
	}
	public void setRequireAbsent(boolean requireAbsent) {
		this.requireAbsent = requireAbsent;
	}

	public boolean orthoClusterSatisfiesCriterion(GeneRegulationParams geneRegulationParams, OrthoCluster orthoCluster) {
		List<SpeciesGene> genes = orthoCluster.getSpeciesToGenes().get(species);
		if(requireAbsent && genes != null && !genes.isEmpty()) {
			return false;
		} else if(requirePresent && (genes == null || genes.isEmpty())) {
			return false;
		}
		if(requireUpregulated && 
				( genes == null || !genes.stream().anyMatch(geneRegulationParams::upregulatedGene) )) {
			return false;
		} else if(requireNotUpregulated && 
				( genes != null && genes.stream().anyMatch(geneRegulationParams::upregulatedGene) )) {
			return false;
		}
		if(requireDownregulated && 
				( genes == null || !genes.stream().anyMatch(geneRegulationParams::downregulatedGene) )) {
			return false;
		} else if(requireNotDownregulated && 
				( genes != null && genes.stream().anyMatch(geneRegulationParams::downregulatedGene) )) {
			return false;
		}
		return true;
	}
	
}
