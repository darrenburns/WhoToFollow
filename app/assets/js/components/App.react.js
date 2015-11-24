import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import {Route, Router, Link} from 'react-router';
import Home from './Home.react';
import QueryResults from './QueryResults.react';

const App = React.createClass({

    render() {
        return (
            <Container maxWidth={800}>
                {this.props.children}
            </Container>
        )
    }

});

ReactDOM.render((
    <Router>
        <Route path="/" component={App}>
            <Route path="home" component={Home} />
            <Route path="query/:query" component={QueryResults} />
        </Route>
    </Router>
), document.getElementById("wtfc-app-mount"));