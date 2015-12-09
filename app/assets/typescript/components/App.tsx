import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import {History, Route, Router, Link} from 'react-router';
import Home from './Home.tsx';
import QueryResults from './QueryResults.tsx';
import UserInfo from './UserInfo.tsx';
import {AppBar, IconButton, FlatButton} from 'material-ui';
import * as injectTapEventPlugin from 'react-tap-event-plugin';
injectTapEventPlugin();

interface IAppState {
    indexSizeSocket?: WebSocket
    indexSize?: number
}

const App = React.createClass<any, IAppState>({

    mixins: [History],

    getInitialState() {
        return {
            indexSizeSocket: null,
            indexSize: 0
        }
    },


    componentWillMount() {
        let ws: WebSocket = new WebSocket(`ws://localhost:9000/ws/default:index-size`);
        ws.onmessage = event => {
            this.setState({indexSize: JSON.parse(event.data).indexSize});
        };
        if (this.state.indexSizeSocket != null) {
            this.state.indexSizeSocket.close();
        }
        this.setState({
            indexSizeSocket: ws
        });
    },

    _onClickBackButton(event: Event) {
        this.history.goBack();
    },

    componentWillUnmount() {
        if (this.state.indexSizeSocket != null) {
            this.state.indexSizeSocket.close();
        }
    },

    render() {
        return (
            <Container maxWidth={900}>
                <AppBar
                    title="WhoToFollow"
                    iconElementLeft={<IconButton iconClassName="material-icons" tooltipPosition="bottom-center"
                                        tooltip="Back" onClick={this._onClickBackButton}>arrow_back</IconButton>}
                    iconElementRight={<FlatButton label={this.state.indexSize + " tweets indexed"}  />}
                />
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