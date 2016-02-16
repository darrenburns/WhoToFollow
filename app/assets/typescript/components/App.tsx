import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as Immutable from 'immutable';
import {Container, Row, Col} from 'elemental';
import {History, Route, Router, Link} from 'react-router';
import Home from './Home.tsx';
import QueryResults from './QueryResults.tsx';
import {AppBar, IconButton, FlatButton} from 'material-ui';
import * as injectTapEventPlugin from 'react-tap-event-plugin';
import Props = __React.Props;
import TwitterUserPreviewPane from "./TwitterUserPreviewPane";

injectTapEventPlugin();

export interface RecentQuery {
    query: string
    id: number
    timestamp: number
}

interface IAppState {
    metricsSocket?: WebSocket
    recentQueries?: Array<RecentQuery>
    indexSize?: number
}

const App = React.createClass<any, IAppState>({

    mixins: [History],

    getInitialState() {
        return {
            metricsSocket: null,
            indexSize: 0,
            recentQueries: Array<RecentQuery>()
        }
    },


    componentDidMount() {
        let ws: WebSocket = new WebSocket(`ws://localhost:9000/ws/main/`);

        // Register event listener on WebSocket
        ws.onmessage = event => {
            let eventData = JSON.parse(event.data);
            if (eventData.hasOwnProperty('numDocs')) {
                this.setState({indexSize: eventData.numDocs});
            }
            // FIXME: Handle this correctly - queries in this event don't have exact same format
            //if (eventData.hasOwnProperty('recentQueries')) {
            //    this.setState({recentQueries: eventData.recentQueries});
            //}
            if (eventData.hasOwnProperty('query')) {
                this.setState({recentQueries: [eventData].concat(this.state.recentQueries).slice(0, 5)});
            }
        };
        if (this.state.metricsSocket != null) {
            this.state.metricsSocket.close();
        }
        this.setState({
            metricsSocket: ws
        });
    },

    componentWillUnmount() {
        if (this.state.metricsSocket != null) {
            this.state.metricsSocket.close();
        }
    },

    _onClickBackButton(event: Event) {
        this.history.goBack();
    },

    render() {
        return (
            <Container maxWidth={1000}>
                <AppBar
                    title="Who To Follow"
                    iconElementLeft={<IconButton iconClassName="material-icons" tooltipPosition="bottom-center"
                                        tooltip="Back" onClick={this._onClickBackButton}>arrow_back</IconButton>}
                    iconElementRight={<FlatButton label={this.state.indexSize + " users indexed"}  />}
                />
                {this.props.children && React.cloneElement(this.props.children, {
                    recentQueries: this.state.recentQueries
                    })}
            </Container>
        )
    }

});

ReactDOM.render((
    <Router>
        <Route component={App}>
            <Route path="/" component={Home}>
                <Route path="/query/:query" component={QueryResults}>
                    <Route path="/query/:query/user/:screenName" component={TwitterUserPreviewPane} />
                </Route>
            </Route>
        </Route>
    </Router>
), document.getElementById("wtfc-app-mount"));