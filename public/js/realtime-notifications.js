
function init() {

    registerHandlerForUpdateCurrentPriceAndFeed();
};



function registerHandlerForUpdateCurrentPriceAndFeed() {
    var eventBus = new EventBus('http://localhost:7070/eventbus');
    eventBus.onopen = function () {
        eventBus.registerHandler('not', function (error, message) {
            alert(message.body);
        });
    }
};


