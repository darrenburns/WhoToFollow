import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import {Route, Router, Link} from 'react-router';
import Home from './Home.tsx';
import QueryResults from './QueryResults.tsx';
import UserInfo from './UserInfo.tsx';

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
        <Route component={App}>
            <Route path="home" component={Home} />
            <Route path="query/:query" component={QueryResults} />
            <Route path="user/:screenName/query/:query" component={UserInfo} />
        </Route>
    </Router>
), document.getElementById("wtfc-app-mount"));