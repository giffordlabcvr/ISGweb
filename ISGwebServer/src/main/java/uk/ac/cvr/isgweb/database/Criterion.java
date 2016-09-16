package uk.ac.cvr.isgweb.database;

import java.util.List;
import java.util.Map;

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

	private boolean requireUpregulated;
	private boolean requireDownregulated;
	private boolean requireNotDifferentiallyExpressed;
	
	private Double upregulatedMinLog2FC; // null if presence == ABSENT or requireUpregulated = false, non-null otherwise.
	private Double downregulatedMaxLog2FC; // null if presence == ABSENT or requireDownregulated = false, non-null otherwise.
	private Double upregulatedMaxFDR; // null if presence == ABSENT or requireUpregulated = false, non-null otherwise.
	public Double getUpregulatedMaxFDR() {
		return upregulatedMaxFDR;
	}
	public void setUpregulatedMaxFDR(Double upregulatedMaxFDR) {
		this.upregulatedMaxFDR = upregulatedMaxFDR;
	}
	public Double getDownregulatedMaxFDR() {
		return downregulatedMaxFDR;
	}
	public void setDownregulatedMaxFDR(Double downregulatedMaxFDR) {
		this.downregulatedMaxFDR = downregulatedMaxFDR;
	}

	private Double downregulatedMaxFDR; // null if presence == ABSENT or requireDownregulated = false, non-null otherwise.
	
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

	
	
	
	public boolean isRequireUpregulated() {
		return requireUpregulated;
	}
	public void setRequireUpregulated(boolean requireUpregulated) {
		this.requireUpregulated = requireUpregulated;
	}
	public boolean isRequireDownregulated() {
		return requireDownregulated;
	}
	public void setRequireDownregulated(boolean requireDownregulated) {
		this.requireDownregulated = requireDownregulated;
	}
	public boolean isRequireNotDifferentiallyExpressed() {
		return requireNotDifferentiallyExpressed;
	}
	public void setRequireNotDifferentiallyExpressed(
			boolean requireNotDifferentiallyExpressed) {
		this.requireNotDifferentiallyExpressed = requireNotDifferentiallyExpressed;
	}
	public Double getUpregulatedMinLog2FC() {
		return upregulatedMinLog2FC;
	}
	public void setUpregulatedMinLog2FC(Double upregulatedMinLog2FC) {
		this.upregulatedMinLog2FC = upregulatedMinLog2FC;
	}
	public Double getDownregulatedMaxLog2FC() {
		return downregulatedMaxLog2FC;
	}
	public void setDownregulatedMaxLog2FC(Double downregulatedMaxLog2FC) {
		this.downregulatedMaxLog2FC = downregulatedMaxLog2FC;
	}
	
	private boolean geneListMatches(List<SpeciesGene> speciesGenes) {
		if(requireUpregulated && !speciesGenes.stream().anyMatch(this::upregulatedGene)) {
			return false;
		}
		if(requireDownregulated && !speciesGenes.stream().anyMatch(this::downregulatedGene)) {
			return false;
		}
		if(requireNotDifferentiallyExpressed && !speciesGenes.stream().anyMatch(this::nonDifferentiallyExpressedGene)) {
			return false;
		}
		return true;
	}
	
	private boolean upregulatedGene(SpeciesGene gene) {
		return gene.getIsDifferentiallyExpressed() && gene.getLog2foldChange() > upregulatedMinLog2FC && gene.getFdr() < upregulatedMaxFDR;
	}

	private boolean downregulatedGene(SpeciesGene gene) {
		return gene.getIsDifferentiallyExpressed() && gene.getLog2foldChange() < downregulatedMaxLog2FC && gene.getFdr() < downregulatedMaxFDR;
	}

	private boolean nonDifferentiallyExpressedGene(SpeciesGene gene) {
		return gene.getLog2foldChange() == null;
	}

	public boolean orthoClusterMatches(OrthoCluster orthoCluster) {
		Map<Species, List<SpeciesGene>> speciesToGenes = orthoCluster.getSpeciesToGenes();
		switch(presence) {
		case PRESENT: {
			switch(speciesCategory) {
			case ALL: {
				if(speciesToGenes.keySet().size() < Species.values().length) {
					return false;
				}
				return speciesToGenes.values().stream().allMatch(this::geneListMatches);
			}
			case ANY: {
				if(speciesToGenes.isEmpty()) {
					return false;
				}
				return speciesToGenes.values().stream().anyMatch(this::geneListMatches);
			}
			case SPECIFIC: {
				List<SpeciesGene> speciesGenes = speciesToGenes.get(species);
				if(speciesGenes == null || speciesGenes.isEmpty()) {
					return false;
				}
				return geneListMatches(speciesGenes);
			}
			}
		}
		case ABSENT: {
			switch(speciesCategory) {
			case ALL: {
				return speciesToGenes.keySet().size() < Species.values().length;
			}
			case ANY: {
				return speciesToGenes.isEmpty();
			}
			case SPECIFIC: {
				List<SpeciesGene> speciesGenes = speciesToGenes.get(species);
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
