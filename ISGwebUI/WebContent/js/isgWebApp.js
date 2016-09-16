var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.resultRows = null;
	  $scope.species = {};
	  $scope.selectedSpecies = null;
	  $scope.selectedPresence = 'PRESENT';
	  $scope.selectedSpeciesCategory = 'ANY';
	  $scope.criteria = [];
	  
	  $scope.availableClustersPerPage = [10,25,100,500];
	  
	  $scope.clustersPerPage = $scope.availableClustersPerPage[0];
	  
	  
	  $scope.resetDifferentialExpression = function() {
		  $scope.includeUpregulated = true;
		  $scope.includeDownregulated = false;
		  $scope.includeNotDifferentiallyExpressed = false;
	  };

	  $scope.resetDifferentialExpression();
	  
		$scope.$watch( 'selectedSpeciesCategory', function(newObj, oldObj) {
			console.log('selectedSpeciesCategory', newObj);
		}, false);
		$scope.$watch( 'selectedPresence', function(newObj, oldObj) {
			console.log('selectedPresence', newObj);
			if(newObj == 'ABSENT') {
				  $scope.includeUpregulated = false;
				  $scope.includeDownregulated = false;
				  $scope.includeNotDifferentiallyExpressed = false;
				  $scope.setSpeciesCategory('SPECIFIC', $scope.defaultSpecies);
			} else {
				  $scope.resetDifferentialExpression();
				  $scope.setSpeciesCategory('ANY', null);
			}
		}, false);
		$scope.$watch( 'selectedSpecies', function(newObj, oldObj) {
			console.log('selectedSpecies', newObj);
		}, false);
		$scope.$watch( 'includeUpregulated', function(newObj, oldObj) {
			console.log('includeUpregulated', newObj);
		}, false);

	  
	  $http.get("../../ISGwebServer/species")
	    .success(function(data, status, headers, config) {
			  console.info('success', data);
			  $scope.defaultSpecies = data.species[0];
			  _.each(data.species, function(s) {
				 $scope.species[s.id] = s; 
			  });
			  console.info('selectedSpecies', $scope.selectedSpecies);
			  console.info('species', $scope.species);
			  
	    })
	    .error(function(data, status, headers, config) {
			  console.info('error', data);
	    });
	  $scope.setSpeciesCategory = function(category, species) {
			 $scope.selectedSpeciesCategory = category;
			 $scope.selectedSpecies = species;
	  }
	  
	  $scope.renderSpeciesCategory = function() {
		  if($scope.selectedSpeciesCategory == 'ANY') {
			  return "Any species"
		  }
		  if($scope.selectedSpeciesCategory == 'ALL') {
			  return "All species"
		  }
		  if($scope.selectedSpeciesCategory == 'SPECIFIC') {
			  return $scope.selectedSpecies.displayName;
		  }
	  }
	  
	  $scope.renderCriterionPresenceAbsenceSpecies = function(criterion) {
		  var presencePart = criterion.presence == 'PRESENT' ? 'Present in' : 'Absent from';
		  var speciesPart;
		  if(criterion.speciesCategory == 'ANY') {
			  speciesPart = "any species";
		  } else if(criterion.speciesCategory == 'ALL') {
			  speciesPart = "all species";
		  } else {
			  speciesPart = $scope.species[criterion.speciesId].displayName;
		  }
		  return presencePart + " " + speciesPart;
	  }

	  $scope.renderIncludeUpregulated = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.includeUpregulated ? 'Yes' : 'No';
	  }

	  $scope.renderIncludeDownregulated = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.includeDownregulated ? 'Yes' : 'No';
	  }

	  $scope.renderIncludeNotDifferentiallyExpressed = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.includeNotDifferentiallyExpressed ? 'Yes' : 'No';
	  }

	  $scope.setAvailableClustersPerPage = function(newValue) {
		  $scope.clustersPerPage = newValue;
	  }
	  
	  $scope.addCriterion = function() {
		  var criterion = { 
					 presence:$scope.selectedPresence,
					 speciesCategory: $scope.selectedSpeciesCategory,
					 includeUpregulated: $scope.includeUpregulated,
					 includeDownregulated: $scope.includeDownregulated,
					 includeNotDifferentiallyExpressed: $scope.includeNotDifferentiallyExpressed
				 };
		  if($scope.selectedSpecies) {
			  criterion["speciesId"] = $scope.selectedSpecies.id;
		  }
		  
		 $scope.criteria.push(criterion); 
	  }

	  $scope.clearCriteria = function() {
		 $scope.criteria = []; 
	  }

	  
	  $scope.runQuery = function() {
		  $http.post("../../ISGwebServer/queryIsgs", {criteria: $scope.criteria})
		    .success(function(data, status, headers, config) {
				  console.info('success', data);
				  var resultRows = [];
				  var orthoClusters = data.orthoClusters;
				  var oddCluster = true;
				  _.each(orthoClusters, function(orthoCluster) {
					  var orthoClusterId = orthoCluster.orthoClusterIdShort;
					  var speciesToGenes = orthoCluster.speciesToGenes;
					  var firstRowInCluster = null;
					  var numRowsInCluster = 0;
					  _.each(speciesToGenes, function(genes, species) {
						  var firstRowInSpecies = null;
						  var numRowsInSpecies = 0;
						  _.each(genes, function(gene) {
							  var row = {
									  ensemblId: gene[0],
									  geneName: gene[1] == null ? '-' : gene[1],
									  dndsRatio: gene[2] == null ? '-' : gene[2],
									  log2fc: gene[3] == null ? '-' : gene[3],
									  fdr: gene[4] == null ? '-' : gene[4],
									  percId: gene[5] == null ? '-' : gene[5],
									  rowClass: oddCluster ? 'active' : 'normal'
								  };
							  if(firstRowInSpecies == null) {
								  firstRowInSpecies = row;
							  }
							  if(firstRowInCluster == null) {
								  firstRowInCluster = row;
							  }
							  resultRows.push(row);
							  numRowsInSpecies++;
							  numRowsInCluster++;
						  });
						  firstRowInSpecies["species"] = $scope.species[species].displayName;
						  firstRowInSpecies["numRowsInSpecies"] = numRowsInSpecies;
					  });
					  firstRowInCluster["orthoClusterId"] = orthoClusterId;
					  firstRowInCluster["numRowsInCluster"] = numRowsInCluster;
					  oddCluster = !oddCluster
				  });
				  $scope.resultRows = resultRows;
		    })
		    .error(function(data, status, headers, config) {
				  console.info('error', data);
		    });
	  }

	  
	  
  } ]);


