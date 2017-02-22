	  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
	  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
	  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
	  })(window,document,'script','https:www.google-analytics.com/analytics.js','ga');
	
	  console.log("document.location.hostname", document.location.hostname);
	  var trackingID;
	  if(document.location.hostname.indexOf("isg.data.cvr.ac.uk") >= 0) {
		  // production analytics account
		  trackingID = 'UA-92347003-1';
	  } else {
		  // sandbox analytics account
		  trackingID = 'UA-92330357-1';
	  }
	  ga('create', trackingID, 'auto');
	  
var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','ngAnimate','dialogs.main','autocomplete','ngFileSaver',
                                             'angulartics', 'angulartics.google.analytics','angular-cookie-law']);


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
  [ '$scope', '$http', 'dialogs', 'FileSaver', 'Blob', '$analytics',
function ($scope, $http, dialogs, FileSaver, Blob, $analytics) {

	  $scope.resultRows = null;
	  $scope.orthoClusters = null;

	  $scope.species = {};
	  $scope.speciesCriteria = [];
	  $scope.geneRegulationParams = null;
	  
	  $scope.searchByGeneOrEnsemblId = true;
	  $scope.searchByPresenceExpressionSpecific = false;
	  $scope.searchByPresenceExpressionNumber = false;
	  $scope.editGeneExpressionParams = false;
	  
	  $scope.geneOrEnsemblQuery = "";
	  $scope.geneOrEnsemblSuggestions = [];
	  
	  $scope.firstClusterIndex = null;
	  $scope.lastClusterIndex = null;
	  $scope.availableClustersPerPage = [10,25,100,500];
	  $scope.clustersPerPage = $scope.availableClustersPerPage[0];
	  
	  $scope.downloadAnalyticsEvent = null;
	  $scope.downloadAnalyticsLabel = null;
	  
	  $scope.updating = false;
	  
	  $scope.resetGeneRegulationParams = function() {
		  $scope.geneRegulationParams = {
				  upregulatedMinLog2FC : 0.0,
				  downregulatedMaxLog2FC : 0.0,
				  maxFDR : 0.05
		  };
	  };

	  $scope.resetGeneRegulationParams();

	  $scope.resetSearchByNumberCriteria = function() {
		  var numSpecies = Object.keys($scope.species).length;
		  $scope.searchByNumberCriteria = {
				  presentMin: 0,
				  presentMax: numSpecies,
				  upRegulatedPresentMin: 0,
				  upRegulatedPresentMax: numSpecies,
				  downRegulatedPresentMin: 0,
				  downRegulatedPresentMax: numSpecies
		  };
	  }
	  
	  
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
		 if(min != null && value < min) {
			 return defaultValue;
		 }
		 if(max != null && value > max) {
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
			  $scope.resetSearchByNumberCriteria();

			  
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
			 $scope.downloadAnalyticsEvent = null;
			 $scope.downloadAnalyticsLabel = null;
	  }
	  
	  $scope.downloadResults = function(fileType) {
		  var lineFeedStyle = "lf";
		  if(userAgent.os.family.indexOf("Windows") !== -1) {
			  lineFeedStyle = "crlf";
		  }
		  
		  $analytics.eventTrack($scope.downloadAnalyticsEvent, {  category: 'clusterDownload', label: $scope.downloadAnalyticsLabel });
		  
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
		  
		  var numSpecies = Object.keys($scope.species).length;
		  $scope.searchByNumberCriteria.presentMin = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.presentMin, 0));
		  $scope.searchByNumberCriteria.presentMax = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.presentMax, numSpecies));
		  $scope.searchByNumberCriteria.upRegulatedPresentMin = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.upRegulatedPresentMin, 0));
		  $scope.searchByNumberCriteria.upRegulatedPresentMax = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.upRegulatedPresentMax, numSpecies));
		  $scope.searchByNumberCriteria.downRegulatedPresentMin = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.downRegulatedPresentMin, 0));
		  $scope.searchByNumberCriteria.downRegulatedPresentMax = 
			  Number($scope.validateParam(0, numSpecies, $scope.searchByNumberCriteria.downRegulatedPresentMax, numSpecies));
	  }
	  
	  
	  $scope.runSpeciesCriteriaQuery = function() {
		  
		  $scope.validateParams();
		  
		  var analyticsLabel = "";
		  if($scope.speciesCriteria.length == 0) {
			  analyticsLabel = "noSpeciesMentioned";
		  } else {
			  var species = [];
			  _.each($scope.speciesCriteria, function(speciesCriterion) {
				  var presExp = "";
				  if(speciesCriterion.requireAbsent) {
					  presExp += "P-"
				  }
				  if(speciesCriterion.requirePresent) {
					  presExp += "P+"
				  }
				  if(speciesCriterion.requireUpregulated) {
					  presExp += "U+"
				  }
				  if(speciesCriterion.requireNotUpregulated) {
					  presExp += "U-"
				  }
				  if(speciesCriterion.requireDownregulated) {
					  presExp += "D+"
				  }
				  if(speciesCriterion.requireNotDownregulated) {
					  presExp += "D-"
				  }
				  if(presExp.length > 0) {
					  species.push(speciesCriterion.species+":"+presExp);
				  }
			  });
			  _.sortBy(species, function(x) {return x;});
			  analyticsLabel = "speciesMentioned:"+species.join();
		  }
		  
		  $scope.downloadAnalyticsEvent = "specificSpeciesClusterDownload";
		  $scope.downloadAnalyticsLabel = analyticsLabel;
		  
		  $analytics.eventTrack('specificSpeciesClusterQuery', {  category: 'clusterQuery', label: analyticsLabel });
		  
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

	  $scope.runSearchByNumberQuery = function() {
		  
		  $scope.validateParams();

		  var cr = $scope.searchByNumberCriteria;
		  
		  var analyticsLabel = "present["+cr.presentMin+":"+cr.presentMax+"],"
		  +"upReg["+cr.upRegulatedPresentMin+":"+cr.upRegulatedPresentMax+"],"
		  +"downReg["+cr.downRegulatedPresentMin+":"+cr.downRegulatedPresentMax+"]";
		  
		  $scope.downloadAnalyticsEvent = "numberSpeciesClusterDownload";
		  $scope.downloadAnalyticsLabel = analyticsLabel;

		  $analytics.eventTrack('numberSpeciesClusterQuery', {  category: 'clusterQuery', label: analyticsLabel });
		  
		  $http.post("../../ISGwebServer/queryByNumber", {
			  geneRegulationParams: $scope.geneRegulationParams,
			  searchByNumberCriteria: $scope.searchByNumberCriteria
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

		  var analyticsLabel = "geneQuery:"+$scope.geneOrEnsemblQuery;
		  
		  $scope.downloadAnalyticsEvent = "specificGeneClusterDownload";
		  $scope.downloadAnalyticsLabel = analyticsLabel;
		  
		  $analytics.eventTrack('specificGeneClusterQuery', {  category: 'clusterQuery', label: analyticsLabel });
		  
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


