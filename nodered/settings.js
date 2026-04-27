module.exports = {
    uiPort: process.env.PORT || 1880,
    flowFile: 'flows.json',
    httpStatic: '/data/static',
    functionGlobalContext: {
        amqplib: require('amqplib'),
        ws: require('ws')
    },
    editorTheme: {
        projects: {
            enabled: false
        }
    }
};
