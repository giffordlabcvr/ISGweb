package uk.ac.cvr.isgweb.model;

public enum Species {

	homo_sapiens("Homo Sapiens", "Human", "HS"),
	rattus_norvegicus("Rattus Norvegicus", "Brown Rat", "RN"),
	bos_taurus("Bos Taurus", "Cattle", "BT"),
	ovis_aries("Ovis Aries", "Sheep", "OA"),
	sus_scrofa("Sus Scrofa", "Pig", "SS"),
	equus_caballus("Equus Caballus", "Horse", "EC"),
	canis_familiaris("Canis Familiaris", "Dog", "CF"),
	pteropus_vampyrus("Pteropus Vampyrus", "Large Flying Fox", "PV"),
	myotis_lucifugus("Myotis Lucifugus", "Little Brown Bat", "ML"),
	gallus_gallus("Gallus Gallus", "Chicken", "GG");
	
	private String displayLatinName;
	private String displayCommonName;
	private String abbreviation;
	
	private Species(String displayLatinName, String displayCommonName, String abbreviation) {
		this.displayLatinName = displayLatinName;
		this.displayCommonName = displayCommonName;
		this.abbreviation = abbreviation;
	}

	public String getDisplayLatinName() {
		return displayLatinName;
	}

	public String getDisplayCommonName() {
		return displayCommonName;
	}

	public String getAbbreviation() {
		return abbreviation;
	}
	
}
