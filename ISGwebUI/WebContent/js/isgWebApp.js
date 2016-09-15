var isgWebApp = angular.module('isgWebApp', ['ui.bootstrap','dialogs.main']);


isgWebApp.controller('isgWebAppCtrl', 
  [ '$scope', '$http', 'dialogs',
function ($scope, $http, dialogs) {

	  $scope.result = null;
	  
	  $scope.update = function() {
		  $http.post("../../ISGwebServer/queryIsgs", {someQuestion: "what is the thing?"})
		    .success(function(data, status, headers, config) {
				  console.info('success', data);
		    })
		    .error(function(data, status, headers, config) {
				  console.info('error', data);
		    });
	  }
	  
  } ]);


