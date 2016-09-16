package uk.ac.cvr.isgweb.database;

import uk.ac.cvr.isgweb.model.SpeciesGene;

public class GeneRegulationParams {

	private double upregulatedMinLog2FoldChange;
	private double downregulatedMaxLog2FoldChange;
	private double maxFDR;
	
	public void setUpregulatedMinLog2FoldChange(double upregulatedMinLog2FoldChange) {
		this.upregulatedMinLog2FoldChange = upregulatedMinLog2FoldChange;
	}
	public void setDownregulatedMaxLog2FoldChange(
			double downregulatedMaxLog2FoldChange) {
		this.downregulatedMaxLog2FoldChange = downregulatedMaxLog2FoldChange;
	}
	public void setMaxFDR(double maxFDR) {
		this.maxFDR = maxFDR;
	}
	
	public boolean upregulatedGene(SpeciesGene gene) {
		return gene.getLog2foldChange() != null && 
				gene.getLog2foldChange() > upregulatedMinLog2FoldChange && 
				gene.getFdr() != null &&
				gene.getFdr() < maxFDR;
	}

	public boolean downregulatedGene(SpeciesGene gene) {
		return gene.getLog2foldChange() != null && 
				gene.getLog2foldChange() < downregulatedMaxLog2FoldChange &&
				gene.getFdr() != null &&
				gene.getFdr() < maxFDR;
	}

}
