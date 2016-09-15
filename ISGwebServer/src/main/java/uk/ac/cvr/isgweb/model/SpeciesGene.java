package uk.ac.cvr.isgweb.model;

public class SpeciesGene {

	private Species species;
	private String ensembleId;
	private String geneName;
	private Double dnDsRatio;
	private Double percentIdentity;
	private Double log2foldChange;
	private Double fdr;
	private Boolean isDifferentiallyExpressed = true;
	private Boolean anyHomology = true;
	
	public Species getSpecies() {
		return species;
	}
	public void setSpecies(Species species) {
		this.species = species;
	}
	public String getEnsembleId() {
		return ensembleId;
	}
	public void setEnsembleId(String ensembleId) {
		this.ensembleId = ensembleId;
	}
	public String getGeneName() {
		return geneName;
	}
	public void setGeneName(String geneName) {
		this.geneName = geneName;
	}
	public Double getDnDsRatio() {
		return dnDsRatio;
	}
	public void setDnDsRatio(Double dnDsRatio) {
		this.dnDsRatio = dnDsRatio;
	}
	public Double getPercentIdentity() {
		return percentIdentity;
	}
	public void setPercentIdentity(Double percentIdentity) {
		this.percentIdentity = percentIdentity;
	}
	public Double getLog2foldChange() {
		return log2foldChange;
	}
	public void setLog2foldChange(Double log2foldChange) {
		this.log2foldChange = log2foldChange;
	}
	public Double getFdr() {
		return fdr;
	}
	public void setFdr(Double fdr) {
		this.fdr = fdr;
	}
	public Boolean getIsDifferentiallyExpressed() {
		return isDifferentiallyExpressed;
	}
	public void setIsDifferentiallyExpressed(Boolean isDifferentiallyExpressed) {
		this.isDifferentiallyExpressed = isDifferentiallyExpressed;
	}
	public Boolean getAnyHomology() {
		return anyHomology;
	}
	public void setAnyHomology(Boolean anyHomology) {
		this.anyHomology = anyHomology;
	}
	
	
	
	
	
}
