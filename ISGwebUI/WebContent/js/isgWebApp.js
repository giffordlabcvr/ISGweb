var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.resultRows = null;
	  $scope.species = {};
	  $scope.selectedSpecies = null;
	  $scope.selectedPresence = 'PRESENT';
	  $scope.criteria = [];
	  
	  $http.get("../../ISGwebServer/species")
	    .success(function(data, status, headers, config) {
			  console.info('success', data);
			  $scope.selectedSpecies = data.species[0];
			  _.each(data.species, function(s) {
				 $scope.species[s.id] = s; 
			  });
			  console.info('selectedSpecies', $scope.selectedSpecies);
			  console.info('species', $scope.species);
			  
	    })
	    .error(function(data, status, headers, config) {
			  console.info('error', data);
	    });

	  $scope.addCriterion = function() {
		 $scope.criteria.push({ 
			 presence:$scope.selectedPresence,
			 speciesId:$scope.selectedSpecies.id
		 }); 
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


