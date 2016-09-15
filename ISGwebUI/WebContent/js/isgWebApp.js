var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.result = null;
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
		    })
		    .error(function(data, status, headers, config) {
				  console.info('error', data);
		    });
	  }

	  
	  
  } ]);


