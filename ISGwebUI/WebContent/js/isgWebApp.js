var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.resultRows = null;
	  $scope.orthoClusters = null;
	  $scope.species = {};
	  $scope.criteria = [];
	  
	  $scope.firstClusterIndex = null;
	  $scope.lastClusterIndex = null;
	  
	  $scope.availableClustersPerPage = [10,25,100,500];
	  
	  $scope.clustersPerPage = $scope.availableClustersPerPage[0];
	  
	  
	  $scope.resetDifferentialExpression = function() {
		  $scope.requireUpregulated = false;
		  $scope.requireDownregulated = false;
		  $scope.requireNotDifferentiallyExpressed = false;
	  };

	  $scope.resetDifferentialExpression();
	  
	  $scope.resetSpecies = function() {
		  $scope.selectedSpecies = null;
		  $scope.selectedPresence = 'PRESENT';
		  $scope.selectedSpeciesCategory = 'ANY';
	  }

	  $scope.resetSpecies();

		$scope.$watch( 'selectedSpeciesCategory', function(newObj, oldObj) {
			console.log('selectedSpeciesCategory', newObj);
		}, false);
		$scope.$watch( 'selectedPresence', function(newObj, oldObj) {
			console.log('selectedPresence', newObj);
			if(newObj == 'ABSENT') {
				  $scope.requireUpregulated = false;
				  $scope.requireDownregulated = false;
				  $scope.requireNotDifferentiallyExpressed = false;
				  $scope.setSpeciesCategory('SPECIFIC', $scope.defaultSpecies);
			} else {
				  $scope.resetDifferentialExpression();
				  $scope.setSpeciesCategory('ANY', null);
			}
		}, false);
		$scope.$watch( 'selectedSpecies', function(newObj, oldObj) {
			console.log('selectedSpecies', newObj);
		}, false);
		$scope.$watch( 'requireUpregulated', function(newObj, oldObj) {
			console.log('requireUpregulated', newObj);
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
		  var presencePart = criterion.presence == 'PRESENT' ? 'Cluster contains one or more genes in ' : 'Cluster does not contain genes in ';
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

	  $scope.renderRequireUpregulated = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.requireUpregulated ? 'Required' : 'Not required';
	  }

	  $scope.renderRequireDownregulated = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.requireDownregulated ? 'Required' : 'Not required';
	  }

	  $scope.renderRequireNotDifferentiallyExpressed = function(criterion) {
		  if(criterion.presence == 'ABSENT') {
			  return '-';
		  }
		  return criterion.requireNotDifferentiallyExpressed ? 'Required' : 'Not required';
	  }

	  $scope.setAvailableClustersPerPage = function(newValue) {
		  $scope.clustersPerPage = newValue;
	  }
	  
	  $scope.addCriterion = function() {
		  var criterion = { 
					 presence:$scope.selectedPresence,
					 speciesCategory: $scope.selectedSpeciesCategory,
				 };
		  if($scope.selectedSpecies) {
			  criterion["speciesId"] = $scope.selectedSpecies.id;
		  }
		  if($scope.selectedPresence == 'PRESENT') {
			  criterion["requireUpregulated"] = $scope.requireUpregulated;
			  criterion["requireDownregulated"] = $scope.requireDownregulated;
			  criterion["requireNotDifferentiallyExpressed"] = $scope.requireNotDifferentiallyExpressed;
		  }
		  
		 $scope.criteria.push(criterion); 
	  }
	  
	  $scope.removeCriterion = function(index) {
			 $scope.criteria.splice(index, 1); 
	  }
	  

	  $scope.clearCriteria = function() {
			 $scope.criteria = []; 
			  $scope.resetSpecies();
			  $scope.resetDifferentialExpression();

		  }

	  $scope.clearResults = function() {
			 $scope.resultRows = null; 
			 $scope.orthoClusters = null; 
	  }

	  
	  $scope.runQuery = function() {
		  $http.post("../../ISGwebServer/queryIsgs", {criteria: $scope.criteria})
		    .success(function(data, status, headers, config) {
				  console.info('success', data);
				  $scope.orthoClusters = data.orthoClusters;
				  if($scope.orthoClusters.length > 0) {
					  $scope.firstPage();
				  } else {
					  $scope.resultRows = [];
				  }
		    })
		    .error(function(data, status, headers, config) {
				  console.info('error', data);
		    });
	  }

	  $scope.firstPage = function() {
		  $scope.firstClusterIndex = 1;
		  $scope.lastClusterIndex = Math.min($scope.orthoClusters.length, $scope.clustersPerPage);
		  $scope.updateResultRows();
	  }

	  $scope.prevPage = function() {
		  $scope.firstClusterIndex = Math.max($scope.firstClusterIndex - $scope.clustersPerPage, 1);
		  $scope.lastClusterIndex = Math.min($scope.orthoClusters.length, $scope.firstClusterIndex + ($scope.clustersPerPage-1));
		  $scope.updateResultRows();
	  }

	  $scope.nextPage = function() {
		  $scope.firstClusterIndex = Math.min($scope.firstClusterIndex + $scope.clustersPerPage, $scope.orthoClusters.length);
		  $scope.lastClusterIndex = Math.min($scope.orthoClusters.length, $scope.firstClusterIndex + ($scope.clustersPerPage-1));
		  $scope.updateResultRows();
	  }

	  $scope.lastPage = function() {
		  $scope.firstClusterIndex = 1 + ( Math.floor( ($scope.orthoClusters.length-1) / $scope.clustersPerPage) * $scope.clustersPerPage );  
		  $scope.lastClusterIndex = $scope.orthoClusters.length;
		  $scope.updateResultRows();
	  }

		$scope.$watch( 'clustersPerPage', function(newObj, oldObj) {
			console.log('clustersPerPage', newObj);
			if($scope.orthoClusters) {
			  $scope.firstPage();
			}
		}, false);
	  
	  $scope.updateResultRows = function() {
		  var resultRows = [];
		  var oddCluster = true;

		  var pageClusters = $scope.orthoClusters.slice($scope.firstClusterIndex - 1, $scope.lastClusterIndex);
		  
		  _.each(pageClusters, function(orthoCluster) {
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
	  }
	  
  } ]);


