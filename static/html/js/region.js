const searchParams = new URLSearchParams(window.location.search);
const region = searchParams.get('region');

export default region;