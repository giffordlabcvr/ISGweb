var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.resultRows = null;
	  $scope.orthoClusters = null;

	  $scope.species = {};
	  $scope.speciesCriteria = [];
	  $scope.geneRegulationParams = null;
	  
	  $scope.firstClusterIndex = null;
	  $scope.lastClusterIndex = null;
	  $scope.availableClustersPerPage = [10,25,100,500];
	  $scope.clustersPerPage = $scope.availableClustersPerPage[0];
	  
	  $scope.updating = false;
	  
	  $scope.resetGeneRegulationParams = function() {
		  $scope.geneRegulationParams = {
				  upregulatedMinLog2FC : 0.0,
				  downregulatedMaxLog2FC : 0.0,
				  maxFDR : 0.05
		  };
	  };

	  $scope.resetGeneRegulationParams();

	  $scope.setOnAllSpeciesCriteria = function(field) {
		  _.each($scope.speciesCriteria, function(crit) {
			  crit[field] = true;
		  });
	  }

	  $scope.validateParam = function(min, max, value, defaultValue) {
		 if(!$scope.isNumeric(value)) {
			 return defaultValue;
		 }  
		 if(min != null && value <= min) {
			 return defaultValue;
		 }
		 if(max != null && value >= max) {
			 return defaultValue;
		 }
		 return value;
	  }  
	  
	  $scope.isNumeric = function(n) {
		  return !isNaN(parseFloat(n)) && isFinite(n);
	  };
	  
	  $scope.resetSpeciesCriteria = function() {
		  $scope.speciesCriteria = [];
		  var i = 0;
		  _.each($scope.species, function(value, key) {
			  var criterion = {
					  species: key,
					  requireUpregulated : false,
					  requireNotUpregulated : false,
					  requireDownregulated : false,
					  requireNotDownregulated : false,
					  requirePresent: false,
					  requireAbsent: false
				  };
			  $scope.speciesCriteria.push(criterion);
				$scope.$watch( 'speciesCriteria['+i+'].requirePresent', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requireAbsent = false;
						}
						$scope.updating = false;
					}
				}, false);
				$scope.$watch( 'speciesCriteria['+i+'].requireAbsent', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requirePresent = false;
							criterion.requireUpregulated = false;
							criterion.requireDownregulated = false;
						}
						$scope.updating = false;
					}
				}, false);
				$scope.$watch( 'speciesCriteria['+i+'].requireUpregulated', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requireNotUpregulated = false;
							criterion.requireAbsent = false;
						}
						$scope.updating = false;
					}
				}, false);
				$scope.$watch( 'speciesCriteria['+i+'].requireNotUpregulated', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requireUpregulated = false;
						}
						$scope.updating = false;
					}
				}, false);
				$scope.$watch( 'speciesCriteria['+i+'].requireDownregulated', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requireNotDownregulated = false;
							criterion.requireAbsent = false;
						}
						$scope.updating = false;
					}
				}, false);
				$scope.$watch( 'speciesCriteria['+i+'].requireNotDownregulated', function(newObj, oldObj) {
					if(!$scope.updating) {
						$scope.updating = true;
						if(newObj) {
							criterion.requireDownregulated = false;
						}
						$scope.updating = false;
					}
				}, false);
				i++;
		  });
		  console.info('speciesCriteria', $scope.speciesCriteria);

	  };

	  $scope.resetSpeciesCriteria();


	  $http.get("../../ISGwebServer/species")
	    .success(function(data, status, headers, config) {
			  console.info('success', data);
			  _.each(data.species, function(s) {
				 $scope.species[s.id] = s; 
			  });
			  console.info('species', $scope.species);
			  $scope.resetSpeciesCriteria();
			  
	    })
	    .error(function(data, status, headers, config) {
			  console.info('error', data);
	    });
	  
	  
	
	  $scope.setAvailableClustersPerPage = function(newValue) {
		  $scope.clustersPerPage = newValue;
	  }
	  
	  
	  $scope.clearResults = function() {
			 $scope.resultRows = null; 
			 $scope.orthoClusters = null; 
	  }

	  
	  $scope.runQuery = function() {

		  $scope.geneRegulationParams.upregulatedMinLog2FC = 
			  Number($scope.validateParam(0, null, $scope.geneRegulationParams.upregulatedMinLog2FC, 0.0));

		  $scope.geneRegulationParams.downregulatedMaxLog2FC = 
			  Number($scope.validateParam(null, 0, $scope.geneRegulationParams.downregulatedMaxLog2FC, 0.0));

		  $scope.geneRegulationParams.maxFDR = 
			  Number($scope.validateParam(0, null, $scope.geneRegulationParams.maxFDR, 0.05));
		  
		  $http.post("../../ISGwebServer/queryIsgs", {
			  geneRegulationParams: $scope.geneRegulationParams,
			  speciesCriteria: $scope.speciesCriteria
			 })
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


