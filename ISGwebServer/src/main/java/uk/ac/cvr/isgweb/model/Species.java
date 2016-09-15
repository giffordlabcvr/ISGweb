package uk.ac.cvr.isgweb.model;

public enum Species {

	homo_sapiens("Homo Sapiens", "Human"),
	rattus_norvegicus("Rattus Norvegicus", "Brown Rat"),
	bos_taurus("Bos Taurus", "Cattle"),
	ovis_aries("Ovis Aries", "Sheep"),
	sus_scrofa("Sus Scrofa", "Pig"),
	equus_caballus("Equus Caballus", "Horse"),
	canis_familiaris("Canis Familiaris", "Dog"),
	pteropus_vampyrus("Pteropus Vampyrus", "Large Flying Fox"),
	myotis_lucifugus("Myotis Lucifugus", "Little Brown Bat"),
	gallus_gallus("Gallus Gallus", "Chicken");
	
	private String displayLatinName;
	private String displayCommonName;
	
	private Species(String displayLatinName, String displayCommonName) {
		this.displayLatinName = displayLatinName;
		this.displayCommonName = displayCommonName;
	}

	public String getDisplayLatinName() {
		return displayLatinName;
	}

	public String getDisplayCommonName() {
		return displayCommonName;
	}
	
}
