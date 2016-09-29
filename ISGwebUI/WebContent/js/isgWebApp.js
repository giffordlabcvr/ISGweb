var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','ngAnimate','dialogs.main','autocomplete','ngFileSaver']);


var userAgent = detect.parse(navigator.userAgent);

console.log("userAgent.browser.family", userAgent.browser.family);
console.log("userAgent.browser.name", userAgent.browser.name);
console.log("userAgent.browser.version", userAgent.browser.version);

console.log("userAgent.os.family", userAgent.os.family);
console.log("userAgent.os.name", userAgent.os.name);
console.log("userAgent.os.version", userAgent.os.version);
console.log("userAgent.os.major", userAgent.os.major);
console.log("userAgent.os.minor", userAgent.os.minor);
console.log("userAgent.os.patch ", userAgent.os.patch );


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs', 'FileSaver', 'Blob',
function ($scope, $http, dialogs, FileSaver, Blob) {

	  $scope.resultRows = null;
	  $scope.orthoClusters = null;

	  $scope.species = {};
	  $scope.speciesCriteria = [];
	  $scope.geneRegulationParams = null;
	  
	  $scope.searchByGeneOrEnsemblId = true;
	  $scope.searchByPresenceExpression = false;
	  $scope.editGeneExpressionParams = false;
	  
	  $scope.geneOrEnsemblQuery = "";
	  $scope.geneOrEnsemblSuggestions = [];
	  
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

	  $scope.myKeyDown = function(event) {
		    if(event.keyCode == 13) {  
		    	$scope.runGeneOrEnsemblQuery();
		    }
		}
	  
	  $scope.setOnAllSpeciesCriteria = function(field) {
		  // set this field true for all species, except when it already is, in which case set to false for all species.
		  var allTrue = true;
		  _.each($scope.speciesCriteria, function(crit) {
			  if(crit[field] != true) {
				  allTrue = false;
			  }
		  });
		  if(allTrue) {
			  _.each($scope.speciesCriteria, function(crit) {
				  crit[field] = false;
			  });
		  } else {
			  _.each($scope.speciesCriteria, function(crit) {
				  crit[field] = true;
			  });
		  }
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
		 return parseFloat(value);
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
	  
	  $scope.downloadResults = function(fileType) {
		  var lineFeedStyle = "lf";
		  if(userAgent.os.family.indexOf("Windows") !== -1) {
			  lineFeedStyle = "crlf";
		  }
		  
		  $http.post("../../ISGwebServer/clusterResultsAsFile", {
			  orthoClusters: $scope.orthoClusters,
			  fileType: fileType,
			  lineFeedStyle: lineFeedStyle
		  })
		  .success(function(data, status, headers, config) {
			  console.info('success', data);
			  var blob = new Blob([data.content], { type: data.contentType });
			  FileSaver.saveAs(blob, data.fileName);
		  })
		  .error(function(data, status, headers, config) {
			  console.info('error', data);
		  });
	  }

	  $scope.validateParams = function() {
		  $scope.geneRegulationParams.upregulatedMinLog2FC = 
			  Number($scope.validateParam(0, null, $scope.geneRegulationParams.upregulatedMinLog2FC, 0.0));

		  $scope.geneRegulationParams.downregulatedMaxLog2FC = 
			  Number($scope.validateParam(null, 0, $scope.geneRegulationParams.downregulatedMaxLog2FC, 0.0));

		  $scope.geneRegulationParams.maxFDR = 
			  Number($scope.validateParam(0, null, $scope.geneRegulationParams.maxFDR, 0.05));
	  }
	  
	  
	  $scope.runSpeciesCriteriaQuery = function() {
		  
		  $scope.validateParams();
		  
		  $http.post("../../ISGwebServer/queryBySpeciesCriteria", {
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

	  $scope.runGeneOrEnsemblQuery = function() {

		  $scope.validateParams();

		  $http.post("../../ISGwebServer/queryByGeneNameOrEnsemblId", {
			  geneNameOrEnsemblId: $scope.geneOrEnsemblQuery,
			  geneRegulationParams: $scope.geneRegulationParams
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

       
	  $scope.geneOrEnsemblSuggestions = [];

      $scope.updateGeneOrEnsemblSuggestions = function(queryText){
    	  $scope.geneOrEnsemblQuery = queryText;
    	  
    	   $http.post("../../ISGwebServer/suggestGeneOrEnsembl", {
    		   queryText: queryText,
    		   maxHits: 100
    	   })
		    .success(function(data, status, headers, config) {
				  console.info('success', data);
				  $scope.geneOrEnsemblSuggestions = data.hits;
		    })
		    .error(function(data, status, headers, config) {
				  console.info('error', data);
		    });
    	   
       }
	  
      $scope.updateSelectedGeneOrEnsembl = function(selectedText){
		  console.info('updateSelectedGeneOrEnsembl:selectedText', selectedText);
    	  $scope.geneOrEnsemblQuery = selectedText;
    	  $scope.runGeneOrEnsemblQuery();
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
							  rowClass: oddCluster ? 'active' : 'normal',
							  upregulated: gene[8] == true,
							  downregulated: gene[9] == true,
							  not_differentially_expressed: gene[8] == false && gene[9] == false
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
		  console.log("resultRows", $scope.resultRows);
	  }
	  
  } ]);


