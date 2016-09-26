package uk.ac.cvr.isgweb.model;

public enum Species {

	homo_sapiens("Homo sapiens", "Human", "HS"),
	rattus_norvegicus("Rattus norvegicus", "Brown Rat", "RN"),
	bos_taurus("Bos taurus", "Cattle", "BT"),
	ovis_aries("Ovis aries", "Sheep", "OA"),
	sus_scrofa("Sus scrofa", "Pig", "SS"),
	equus_caballus("Equus caballus", "Horse", "EC"),
	canis_familiaris("Canis familiaris", "Dog", "CF"),
	pteropus_vampyrus("Pteropus vampyrus", "Large Flying Fox", "PV"),
	myotis_lucifugus("Myotis lucifugus", "Little Brown Bat", "ML"),
	gallus_gallus("Gallus gallus", "Chicken", "GG");
	
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
