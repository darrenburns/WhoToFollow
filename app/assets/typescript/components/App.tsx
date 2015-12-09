import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import {Route, Router, Link} from 'react-router';
import Home from './Home.tsx';
import QueryResults from './QueryResults.tsx';
import UserInfo from './UserInfo.tsx';
import * as injectTapEventPlugin from 'react-tap-event-plugin';


interface IAppState {
    indexSizeSocket?: WebSocket
    indexSize?: number
}

class App extends React.Component<any, IAppState> {

    constructor(props) {
        super(props);
        injectTapEventPlugin();
        this.state = {
            indexSizeSocket: null,
            indexSize: 0
        };
    }

    componentWillMount() {
        let ws: WebSocket = new WebSocket(`ws://localhost:9000/ws/default:index-size`);
        ws.onmessage = event => {
            this.setState({indexSize: JSON.parse(event.data).indexSize});
            console.log("IndexSize == " + this.state.indexSize);
        };
        if (this.state.indexSizeSocket != null) {
            this.state.indexSizeSocket.close();
        }
        this.setState({
            indexSizeSocket: ws
        });
    }

    componentWillUnmount() {
        if (this.state.indexSizeSocket != null) {
            this.state.indexSizeSocket.close();
        }
    }

    render() {
        return (
            <Container maxWidth={900}>
                <Row>
                    Number of tweets indexed: {this.state.indexSize}
                </Row>
                {this.props.children}
            </Container>
        )
    }

}

ReactDOM.render((
    <Router>
        <Route component={App}>
            <Route path="home" component={Home} />
            <Route path="query/:query" component={QueryResults} />
            <Route path="user/:screenName/query/:query" component={UserInfo} />
        </Route>
    </Router>
), document.getElementById("wtfc-app-mount"));